package io.javalin.config

import io.javalin.compression.CompressionStrategy
import io.javalin.event.EventManager
import io.javalin.http.RequestLogger
import io.javalin.http.SinglePageHandler
import io.javalin.router.InternalRouter
import io.javalin.http.servlet.DefaultTasks
import io.javalin.http.servlet.JavaLangErrorHandler
import io.javalin.http.staticfiles.ResourceHandler
import io.javalin.plugin.PluginManager
import io.javalin.security.AccessManager
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsRouter
import org.eclipse.jetty.server.Server

// @formatter:off
class PrivateConfig(val cfg: JavalinConfig) {

    @JvmField val eventManager = EventManager()
    @JvmField val wsRouter = WsRouter(cfg.routing)
    @JvmField var internalRouter = InternalRouter(wsRouter, eventManager, cfg.routing)
    @JvmField var pluginManager = PluginManager()
    @JvmField var appAttributes: MutableMap<String, Any> = HashMap()
    @JvmField var requestLogger: RequestLogger? = null
    @JvmField var resourceHandler: ResourceHandler? = null
    @JvmField var accessManager: AccessManager? = null
    @JvmField var singlePageHandler = SinglePageHandler()
    @JvmField var wsLogger: WsConfig? = null
    @JvmField var compressionStrategy = CompressionStrategy.GZIP
    @JvmField var servletRequestLifecycle = listOf(DefaultTasks.BEFORE, DefaultTasks.HTTP, DefaultTasks.ERROR, DefaultTasks.AFTER)
    // Jetty
    @JvmField var server: Server? = null


    fun javaLangErrorHandler(handler: JavaLangErrorHandler): PrivateConfig = also {
        this.internalRouter.exceptionMapper.javaLangErrorHandler = handler
    }

}
// @formatter:on
