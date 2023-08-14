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
import io.javalin.plugin.JavalinPlugin
import io.javalin.plugin.PluginConfiguration
import io.javalin.plugin.PluginFactory
import io.javalin.plugin.createUserConfig
import io.javalin.util.Util.firstOrNull
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

class MicrometerConfig : PluginConfiguration {
    @JvmField var registry: MeterRegistry = Metrics.globalRegistry
    @JvmField var tags: Iterable<Tag> = Tags.empty()
    @JvmField var tagExceptionName: Boolean = false
    @JvmField var tagRedirectPaths: Boolean = false
    @JvmField var tagNotFoundMappedPaths: Boolean = false
}

/**
 * [MicrometerPlugin] has a private constructor, use
 * [MicrometerPlugin.create] to create a new instance.
 */
class MicrometerPlugin(config: Consumer<MicrometerConfig>) : JavalinPlugin {

    open class Micrometer : PluginFactory<MicrometerPlugin, MicrometerConfig> {
        override fun create(config: Consumer<MicrometerConfig>) = MicrometerPlugin(config)
    }

    companion object {
        private const val EXCEPTION_HEADER = "__micrometer_exception_name"
        object Micrometer : MicrometerPlugin.Micrometer()
        @JvmField var exceptionHandler = ExceptionHandler { e: Exception, ctx: Context ->
            val simpleName = e.javaClass.simpleName
            ctx.header(EXCEPTION_HEADER, simpleName.ifBlank { e.javaClass.name })
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private val config = config.createUserConfig(MicrometerConfig())

    override fun onStart(app: Javalin) {
        if (config.tagExceptionName) {
            app.exception(Exception::class.java, exceptionHandler)
        }

        app.cfg.jetty.modifyServer { server ->
            server.insertHandler(TimedHandler(config.registry, config.tags, object : DefaultHttpJakartaServletRequestTagsProvider() {
                override fun getTags(request: HttpServletRequest, response: HttpServletResponse): Iterable<Tag> {
                    val exceptionName = if (config.tagExceptionName) {
                        response.getHeader(EXCEPTION_HEADER)
                    } else {
                        "Unknown"
                    }
                    val pathInfo = request.pathInfo.removePrefix(app.cfg.router.contextPath).prefixIfNot("/")
                    response.setHeader(EXCEPTION_HEADER, null)
                    val handlerType = HandlerType.valueOf(request.method)
                    val uri = app.cfg.pvt.internalRouter.findHttpHandlerEntries(handlerType, pathInfo)
                        .map { it.path }
                        .map { if (it == "/" || it.isBlank()) "root" else it }
                        .map { if (!config.tagRedirectPaths && response.status in 300..399) "REDIRECTION" else it }
                        .map { if (!config.tagNotFoundMappedPaths && response.status == 404) "NOT_FOUND" else it }
                        .firstOrNull() ?: "NOT_FOUND"
                    return Tags.concat(
                        super.getTags(request, response),
                        "uri", uri,
                        "exception", exceptionName ?: "None"
                    )
                }
            }))

            JettyServerThreadPoolMetrics(server.threadPool, config.tags).bindTo(config.registry)
            app.events {
                it.serverStarted {
                    JettyConnectionMetrics.addToAllConnectors(server, config.registry, config.tags)
                }
            }
        }
    }

    private fun String.prefixIfNot(prefix: String) = if (this.startsWith(prefix)) this else "$prefix$this"
}
