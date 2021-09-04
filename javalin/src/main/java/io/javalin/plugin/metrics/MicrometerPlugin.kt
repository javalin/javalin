/*
 * Javalin - https://javalin.io
 * Copyright 2020 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.plugin.metrics

import io.javalin.Javalin
import io.javalin.core.plugin.Plugin
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import io.javalin.http.Context
import io.javalin.http.ExceptionHandler
import io.javalin.http.HandlerType
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.http.DefaultHttpServletRequestTagsProvider
import io.micrometer.core.instrument.binder.jetty.JettyConnectionMetrics
import io.micrometer.core.instrument.binder.jetty.JettyServerThreadPoolMetrics
import io.micrometer.core.instrument.binder.jetty.TimedHandler
import org.apache.commons.lang3.StringUtils
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class MicrometerPlugin @JvmOverloads constructor(
    private val registry: MeterRegistry = Metrics.globalRegistry,
    private val tags: Iterable<Tag> = Tags.empty(),
    private val tagExceptionName: Boolean = false,
    private val tagRedirectPaths: Boolean = false,
    private val tagNotFoundMappedPaths: Boolean = false
) : Plugin {
    override fun apply(app: Javalin) {
        Util.ensureDependencyPresent(OptionalDependency.MICROMETER)
        app.jettyServer()?.server()?.let { server ->
            if (tagExceptionName) {
                app.exception(Exception::class.java, EXCEPTION_HANDLER)
            }

            server.insertHandler(TimedHandler(registry, tags, object : DefaultHttpServletRequestTagsProvider() {
                override fun getTags(request: HttpServletRequest, response: HttpServletResponse): Iterable<Tag> {
                    val exceptionName = if (tagExceptionName) {
                        response.getHeader(EXCEPTION_HEADER)
                    } else {
                        "Unknown"
                    }
                    val pathInfo = request.pathInfo.removePrefix(app._conf.contextPath).prefixIfNot("/")
                    response.setHeader(EXCEPTION_HEADER, null)
                    val handlerType = HandlerType.valueOf(request.method)
                    val uri = app.javalinServlet().matcher.findEntries(handlerType, pathInfo).asSequence()
                        .map { it.path }
                        .map { if (it == "/" || it.isBlank()) "root" else it }
                        .map { if (!tagRedirectPaths && response.status in 300..399) "REDIRECTION" else it }
                        .map { if (!tagNotFoundMappedPaths && response.status == 404) "NOT_FOUND" else it }
                        .firstOrNull() ?: "NOT_FOUND"
                    return Tags.concat(
                        super.getTags(request, response),
                        "uri", uri,
                        "exception", exceptionName ?: "None"
                    )
                }
            }))

            JettyServerThreadPoolMetrics(server.threadPool, tags).bindTo(registry)
            app.events {
                it.serverStarted {
                    JettyConnectionMetrics.addToAllConnectors(server, registry, tags)
                }
            }
        }
    }

    companion object {
        private const val EXCEPTION_HEADER = "__micrometer_exception_name"

        var EXCEPTION_HANDLER = ExceptionHandler { e: Exception, ctx: Context ->
            val simpleName = e.javaClass.simpleName
            ctx.header(EXCEPTION_HEADER, if (StringUtils.isNotBlank(simpleName)) simpleName else e.javaClass.name)
            ctx.status(500)
        }
    }

    private fun String.prefixIfNot(prefix: String) = if (this.startsWith(prefix)) this else "$prefix$this"
}
