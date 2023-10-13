package io.javalin.config

import io.javalin.compression.CompressionStrategy
import io.javalin.event.EventManager
import io.javalin.http.RequestLogger
import io.javalin.http.SinglePageHandler
import io.javalin.router.InternalRouter
import io.javalin.http.servlet.DefaultTasks
import io.javalin.http.servlet.JavalinServlet
import io.javalin.router.exception.JavaLangErrorHandler
import io.javalin.http.staticfiles.ResourceHandler
import io.javalin.jetty.JavalinJettyServlet
import io.javalin.plugin.PluginManager
import io.javalin.security.AccessManager
import io.javalin.util.javalinLazy
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsRouter
import jakarta.servlet.Servlet
import org.eclipse.jetty.server.Server

// @formatter:off
class PrivateConfig(val cfg: JavalinConfig) {

    @JvmField val eventManager = EventManager()
    @JvmField val wsRouter = WsRouter(cfg.router)
    @JvmField var internalRouter = InternalRouter(wsRouter, eventManager, cfg.router)
    @JvmField var pluginManager = PluginManager(cfg)
    @JvmField var appAttributes: MutableMap<String, Any> = HashMap()
    @JvmField var requestLogger: RequestLogger? = null
    @JvmField var resourceHandler: ResourceHandler? = null
    @JvmField var accessManager: AccessManager? = null
    @JvmField var singlePageHandler = SinglePageHandler()
    @JvmField var wsLogger: WsConfig? = null
    @JvmField var compressionStrategy = CompressionStrategy.GZIP
    @JvmField var servletRequestLifecycle = listOf(DefaultTasks.BEFORE, DefaultTasks.HTTP, DefaultTasks.ERROR, DefaultTasks.AFTER)
    @JvmField var servlet: Lazy<Servlet> = javalinLazy {
        val httpServlet = JavalinServlet(cfg)
        when {
            internalRouter.allWsHandlers().isNotEmpty() -> JavalinJettyServlet(cfg, httpServlet)
            else -> httpServlet
        }
    }
    // Jetty
    @JvmField var server: Server? = null

    fun javaLangErrorHandler(handler: JavaLangErrorHandler): PrivateConfig = also {
        this.cfg.router.javaLangErrorHandler = handler
    }

}
// @formatter:on
