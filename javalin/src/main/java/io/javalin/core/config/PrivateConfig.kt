package io.javalin.core.config

import io.javalin.core.compression.CompressionStrategy
import io.javalin.core.plugin.Plugin
import io.javalin.core.security.AccessManager
import io.javalin.core.security.SecurityUtil.noopAccessManager
import io.javalin.http.RequestLogger
import io.javalin.http.SinglePageHandler
import io.javalin.http.staticfiles.ResourceHandler
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
    @JvmField var accessManager: AccessManager = AccessManager { handler, ctx, roles -> noopAccessManager(handler, ctx, roles) }
    @JvmField var singlePageHandler = SinglePageHandler()
    @JvmField var sessionHandler: SessionHandler? = null
    @JvmField var wsFactoryConfig: Consumer<JettyWebSocketServletFactory>? = null
    @JvmField var wsLogger: WsConfig? = null
    @JvmField var server: Server? = null
    @JvmField var servletContextHandlerConsumer: Consumer<ServletContextHandler>? = null
    @JvmField var compressionStrategy = CompressionStrategy.GZIP
}
// @formatter:on
