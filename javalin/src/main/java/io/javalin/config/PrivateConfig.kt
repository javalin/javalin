package io.javalin.config

import io.javalin.compression.CompressionStrategy
import io.javalin.http.RequestLogger
import io.javalin.http.SinglePageHandler
import io.javalin.http.staticfiles.ResourceHandler
import io.javalin.plugin.Plugin
import io.javalin.security.SecurityUtil.noopAccessManager
import io.javalin.websocket.WsConfig
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory
import java.util.function.Consumer

// @formatter:off
class PrivateConfig {
    @JvmField var plugins: MutableMap<Class<out Plugin>, Plugin> = LinkedHashMap()
    @JvmField var appAttributes: MutableMap<String, Any> = HashMap()
    @JvmField var requestLogger: RequestLogger? = null
    @JvmField var resourceHandler: ResourceHandler? = null
    @JvmField var accessManager: io.javalin.security.AccessManager =
        io.javalin.security.AccessManager { handler, ctx, roles -> noopAccessManager(handler, ctx, roles) }
    @JvmField var singlePageHandler = SinglePageHandler()
    @JvmField var sessionHandler: SessionHandler? = null
    @JvmField var wsFactoryConfig: Consumer<JettyWebSocketServletFactory>? = null
    @JvmField var wsLogger: WsConfig? = null
    @JvmField var server: Server? = null
    @JvmField var servletContextHandlerConsumer: Consumer<ServletContextHandler>? = null
    @JvmField var compressionStrategy = CompressionStrategy.GZIP
}
// @formatter:on
