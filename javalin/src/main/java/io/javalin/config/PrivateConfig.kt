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
import org.eclipse.jetty.server.Server


// @formatter:off
class PrivateConfig {
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


    fun javaLangErrorHandler(handler: JavaLangErrorHandler) = apply { this.javaLangErrorHandler = handler }
    internal var javaLangErrorHandler: JavaLangErrorHandler = JavaLangErrorHandler { res, error ->
        res.status = INTERNAL_SERVER_ERROR.code
        JavalinLogger.error("Fatal error occurred while servicing http-request", error)
    }
}

// @formatter:on
