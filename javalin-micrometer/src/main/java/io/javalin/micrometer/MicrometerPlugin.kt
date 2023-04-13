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
import java.util.function.Consumer

data class MicrometerConfig(
    @JvmField var registry: MeterRegistry = Metrics.globalRegistry,
    @JvmField var tags: Iterable<Tag> = Tags.empty(),
    @JvmField var tagExceptionName: Boolean = false,
    @JvmField var tagRedirectPaths: Boolean = false,
    @JvmField var tagNotFoundMappedPaths: Boolean = false,
)

/**
 * [MicrometerPlugin] has a private constructor, use
 * [MicrometerPlugin.create] to create a new instance.
 */
class MicrometerPlugin private constructor(
    private val registry: MeterRegistry,
    private val tags: Iterable<Tag>,
    private val tagExceptionName: Boolean,
    private val tagRedirectPaths: Boolean,
    private val tagNotFoundMappedPaths: Boolean,
) : Plugin {

    override fun apply(app: Javalin) {
        app.jettyServer()?.server()?.let { server ->
            if (tagExceptionName) {
                app.exception(Exception::class.java, exceptionHandler)
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

        @JvmField
        var exceptionHandler = ExceptionHandler { e: Exception, ctx: Context ->
            val simpleName = e.javaClass.simpleName
            ctx.header(EXCEPTION_HEADER, simpleName.ifBlank { e.javaClass.name })
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
        }

        @Deprecated("User exceptionHandler instead", ReplaceWith("exceptionHandler"), DeprecationLevel.ERROR)
        var EXCEPTION_HANDLER = exceptionHandler

        @JvmStatic
        fun create(userConfig: Consumer<MicrometerConfig>): MicrometerPlugin {
            val finalConfig = MicrometerConfig()
            userConfig.accept(finalConfig)
            return MicrometerPlugin(
                registry = finalConfig.registry,
                tags = finalConfig.tags,
                tagExceptionName = finalConfig.tagExceptionName,
                tagRedirectPaths = finalConfig.tagRedirectPaths,
                tagNotFoundMappedPaths = finalConfig.tagNotFoundMappedPaths,
            )
        }
    }

    private fun String.prefixIfNot(prefix: String) = if (this.startsWith(prefix)) this else "$prefix$this"
}
