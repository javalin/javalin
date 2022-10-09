/*
 * Javalin - https://javalin.io
 * Copyright 2020 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.micrometer

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.ExceptionHandler
import io.javalin.http.HandlerType
import io.javalin.http.HttpStatus
import io.javalin.plugin.Plugin
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.binder.http.DefaultHttpJakartaServletRequestTagsProvider
import io.micrometer.core.instrument.binder.jetty.JettyConnectionMetrics
import io.micrometer.core.instrument.binder.jetty.JettyServerThreadPoolMetrics
import io.micrometer.jetty11.TimedHandler
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class MicrometerPlugin @JvmOverloads constructor(
    private val registry: MeterRegistry = Metrics.globalRegistry,
    private val tags: Iterable<Tag> = Tags.empty(),
    private val tagExceptionName: Boolean = false,
    private val tagRedirectPaths: Boolean = false,
    private val tagNotFoundMappedPaths: Boolean = false
) : Plugin {
    override fun apply(app: Javalin) {
        app.jettyServer()?.server()?.let { server ->
            if (tagExceptionName) {
                app.exception(Exception::class.java, EXCEPTION_HANDLER)
            }

            server.insertHandler(TimedHandler(registry, tags, object : DefaultHttpJakartaServletRequestTagsProvider() {
                override fun getTags(request: HttpServletRequest, response: HttpServletResponse): Iterable<Tag> {
                    val exceptionName = if (tagExceptionName) {
                        response.getHeader(EXCEPTION_HEADER)
                    } else {
                        "Unknown"
                    }
                    val pathInfo = request.pathInfo.removePrefix(app.cfg.routing.contextPath).prefixIfNot("/")
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
            ctx.header(EXCEPTION_HEADER, simpleName.ifBlank { e.javaClass.name })
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun String.prefixIfNot(prefix: String) = if (this.startsWith(prefix)) this else "$prefix$this"
}
