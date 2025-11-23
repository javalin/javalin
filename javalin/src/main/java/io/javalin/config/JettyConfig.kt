package io.javalin.config

import org.eclipse.jetty.ee10.servlet.ServletContextHandler
import org.eclipse.jetty.ee10.servlet.SessionHandler
import org.eclipse.jetty.ee10.websocket.server.JettyWebSocketServletFactory
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.ThreadPool
import java.util.function.BiFunction
import java.util.function.Consumer

/**
 * Configures the embedded Jetty webserver.
 *
 * @param cfg the parent Javalin Configuration
 * @see [JavalinConfig.jetty]
 */
class JettyConfig(private val cfg: JavalinConfig) {
    //@formatter:off
    @JvmField var defaultHost: String? = null
    @JvmField var defaultPort = 8080
    @JvmField var multipartConfig = MultipartConfig()
    @JvmField var threadPool: ThreadPool? = null
    /** Default HTTP status code when the server had a timeout. */
    // Default HTTP status code when the server had a timeout (Javalin 7 uses 408)
    @JvmField var timeoutStatus = 408
    /** Default HTTP status code when the client closes the connection. */
    // Default HTTP status code when the client closes the connection (Javalin 7 uses 499)
    @JvmField var clientAbortStatus = 499
    //@formatter:on

    /** Configure the jetty [Server]. This is useful if you want to configure Jetty features that are not exposed by Javalin.
     * Consider using the other methods in this class before resorting to this one.
     * It can be called multiple times, and the supplied consumers will be called in order. */
    fun modifyServer(server: Consumer<Server>) {
        cfg.pvt.jetty.serverConsumers.add(server)
    }

    /** Configure the jetty [ServletContextHandler]. The [SessionHandler] can be set here.
     * It can be called multiple times, and the supplied consumers will be called in order. */
    fun modifyServletContextHandler(consumer: Consumer<ServletContextHandler>) {
        cfg.pvt.jetty.servletContextHandlerConsumers.add(consumer)
    }

    /** Configure the jetty [JettyWebSocketServletFactory].
     * It can be called multiple times, and the supplied consumers will be called in order. */
    fun modifyWebSocketServletFactory(wsFactoryConfig: Consumer<JettyWebSocketServletFactory>) {
        cfg.pvt.jetty.wsFactoryConfigs.add(wsFactoryConfig)
    }

    /** Configure the [HttpConfiguration] to be used by the jetty [Server].
     * It can be called multiple times, and the supplied consumers will be called in order. */
    fun modifyHttpConfiguration(httpConfigurationConfig: Consumer<HttpConfiguration>) {
        cfg.pvt.jetty.httpConfigurationConfigs.add(httpConfigurationConfig)
    }

    /** Add a [Connector] to the jetty [Server].
     * It can be called multiple times, and the supplied connectors will be added in order. */
    fun addConnector(connector : BiFunction<Server, HttpConfiguration, Connector>){
        cfg.pvt.jetty.connectors.add(connector)
    }

}
