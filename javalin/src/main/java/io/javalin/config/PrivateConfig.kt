package io.javalin.config

import io.javalin.compression.CompressionStrategy
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.RequestLogger
import io.javalin.http.SinglePageHandler
import io.javalin.http.servlet.DefaultTasks
import io.javalin.http.servlet.JavaLangErrorHandler
import io.javalin.http.staticfiles.ResourceHandler
import io.javalin.security.AccessManager
import io.javalin.util.JavalinLogger
import io.javalin.websocket.WsConfig
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory
import java.lang.Error
import java.util.function.Consumer

// @formatter:off
class PrivateConfig {
    @JvmField var appAttributes: MutableMap<String, Any> = HashMap()
    @JvmField var requestLogger: RequestLogger? = null
    @JvmField var resourceHandler: ResourceHandler? = null
    @JvmField var accessManager: AccessManager? = null
    @JvmField var singlePageHandler = SinglePageHandler()
    @JvmField var sessionHandler: SessionHandler? = null
    @JvmField var wsFactoryConfig: Consumer<JettyWebSocketServletFactory>? = null
    @JvmField var wsLogger: WsConfig? = null
    @JvmField var server: Server? = null
    @JvmField var servletContextHandlerConsumer: Consumer<ServletContextHandler>? = null
    @JvmField var compressionStrategy = CompressionStrategy.GZIP
    @JvmField var servletRequestLifecycle = listOf(DefaultTasks.BEFORE, DefaultTasks.HTTP, DefaultTasks.ERROR, DefaultTasks.AFTER)
    fun javaLangErrorHandler(handler: JavaLangErrorHandler) = apply { this.javaLangErrorHandler = handler }
    internal var javaLangErrorHandler: JavaLangErrorHandler = JavaLangErrorHandler { res, error ->
        res.status = INTERNAL_SERVER_ERROR.code
        JavalinLogger.error("Fatal error occurred while servicing http-request", error)
    }
}

// @formatter:on
