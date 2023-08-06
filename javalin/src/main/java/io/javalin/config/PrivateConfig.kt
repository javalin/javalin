package io.javalin.config

import io.javalin.compression.CompressionStrategy
import io.javalin.event.EventManager
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.RequestLogger
import io.javalin.http.SinglePageHandler
import io.javalin.http.servlet.DefaultTasks
import io.javalin.http.servlet.ErrorMapper
import io.javalin.http.servlet.ExceptionMapper
import io.javalin.http.servlet.JavaLangErrorHandler
import io.javalin.http.staticfiles.ResourceHandler
import io.javalin.jetty.JavalinJettyServlet
import io.javalin.jetty.JettyServer
import io.javalin.plugin.PluginManager
import io.javalin.routing.PathMatcher
import io.javalin.security.AccessManager
import io.javalin.util.JavalinLogger
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsExceptionMapper
import io.javalin.websocket.WsPathMatcher
import org.eclipse.jetty.server.Server

// @formatter:off
class PrivateConfig(val cfg: JavalinConfig) {
    @JvmField val pathMatcher = PathMatcher()
    @JvmField val exceptionMapper = ExceptionMapper(this)
    @JvmField val errorMapper = ErrorMapper()
    @JvmField val eventManager = EventManager()
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
    val wsExceptionMapper = WsExceptionMapper()
    val wsPathMatcher = WsPathMatcher()

    internal var javaLangErrorHandler: JavaLangErrorHandler = JavaLangErrorHandler { res, error ->
        res.status = INTERNAL_SERVER_ERROR.code
        JavalinLogger.error("Fatal error occurred while servicing http-request", error)
    }

    fun javaLangErrorHandler(handler: JavaLangErrorHandler): PrivateConfig =
        also { this.javaLangErrorHandler = handler }

}
// @formatter:on
