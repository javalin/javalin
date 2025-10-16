package io.javalin.config

import io.javalin.compression.CompressionStrategy
import io.javalin.event.EventManager
import io.javalin.http.RequestLogger
import io.javalin.http.SinglePageHandler
import io.javalin.http.servlet.DefaultTasks
import io.javalin.http.servlet.JavalinServlet
import io.javalin.http.servlet.ServletEntry
import io.javalin.http.staticfiles.ResourceHandler
import io.javalin.http.util.AsyncExecutor
import io.javalin.jetty.JettyUtil.createJettyServletWithWebsocketsIfAvailable
import io.javalin.json.JavalinJackson
import io.javalin.json.JsonMapper
import io.javalin.plugin.PluginManager
import io.javalin.router.InternalRouter
import io.javalin.router.exception.JavaLangErrorHandler
import io.javalin.util.ConcurrencyUtil
import io.javalin.util.javalinLazy
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsRouter

// @formatter:off
class PrivateConfig(val cfg: JavalinConfig) {

    @JvmField val eventManager = EventManager()
    @JvmField val wsRouter = WsRouter(cfg.router)
    @JvmField var internalRouter = InternalRouter(wsRouter, eventManager, cfg.router, cfg.jetty)
    @JvmField var appDataManager = AppDataManager()
    @JvmField var pluginManager = PluginManager(cfg)
    @JvmField var jsonMapper: Lazy<JsonMapper> = javalinLazy { JavalinJackson(null, cfg.useVirtualThreads) }
    @JvmField var requestLogger: RequestLogger? = null
    @JvmField var resourceHandler: ResourceHandler? = null
    @JvmField var singlePageHandler = SinglePageHandler()
    @JvmField var wsLogger: WsConfig? = null
    @JvmField var compressionStrategy = CompressionStrategy.GZIP
    @JvmField var asyncExecutor = javalinLazy { AsyncExecutor(ConcurrencyUtil.executorService("JavalinDefaultAsyncThreadPool", cfg.useVirtualThreads)) }
    @JvmField var servletRequestLifecycle = mutableListOf(DefaultTasks.BEFORE, DefaultTasks.BEFORE_MATCHED, DefaultTasks.HTTP, DefaultTasks.AFTER_MATCHED, DefaultTasks.ERROR, DefaultTasks.AFTER)
    @JvmField var servlet: Lazy<ServletEntry> = javalinLazy { createJettyServletWithWebsocketsIfAvailable(cfg) ?: ServletEntry(servlet = JavalinServlet(cfg)) }

    // Jetty
    @JvmField var jetty = JettyInternalConfig()

    fun javaLangErrorHandler(handler: JavaLangErrorHandler): PrivateConfig = also {
        this.cfg.router.javaLangErrorHandler = handler
    }

}
// @formatter:on
