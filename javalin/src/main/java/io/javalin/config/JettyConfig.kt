package io.javalin.config

import io.javalin.jetty.JettyServer
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.util.thread.ThreadPool
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory
import java.util.function.BiFunction
import java.util.function.Consumer

/**  Configures the embedded Jetty webserver. */
class JettyConfig() {
    //@formatter:off
    @JvmField val multipartConfig = MultipartConfig()
    @JvmField var threadPool: ThreadPool = JettyServer.defaultThreadPool()
    //@formatter:on

    var serverConsumers: MutableList<Consumer<Server>> = mutableListOf()
    var httpConfigurationConfigs: MutableList<Consumer<HttpConfiguration>> = mutableListOf()
    var servletContextHandlerConsumers: MutableList<Consumer<ServletContextHandler>> = mutableListOf()
    var wsFactoryConfigs: MutableList<Consumer<JettyWebSocketServletFactory>> = mutableListOf()
    var connectors: MutableList<BiFunction<Server, HttpConfiguration, Connector>> = mutableListOf()


    /** Configure the jetty [Server]. This is useful if you want to configure Jetty features that are not exposed by Javalin.
     * Consider using the other methods in this class before resorting to this one.
     * It can be called multiple times, and the supplied consumers will be called in order. */
    fun modifyServer(server: Consumer<Server>) {
        serverConsumers.add(server)
    }

    /** Configure the jetty [ServletContextHandler]. The [SessionHandler] can be set here.
     * It can be called multiple times, and the supplied consumers will be called in order. */
    fun modifyServletContextHandler(consumer: Consumer<ServletContextHandler>) {
        servletContextHandlerConsumers.add(consumer)
    }

    /** Configure the jetty [JettyWebSocketServletFactory].
     * It can be called multiple times, and the supplied consumers will be called in order. */
    fun modifyJettyWebSocketServletFactory(wsFactoryConfig: Consumer<JettyWebSocketServletFactory>) {
        wsFactoryConfigs.add(wsFactoryConfig)
    }

    /** Configure the [HttpConfiguration] to be used by the jetty [Server].
     * It can be called multiple times, and the supplied consumers will be called in order. */
    fun modifyHttpConfiguration(httpConfigurationConfig: Consumer<HttpConfiguration>) {
        httpConfigurationConfigs.add(httpConfigurationConfig)
    }

    /** Add a [Connector] to the jetty [Server].
     * It can be called multiple times, and the supplied connectors will be added in order. */
    fun addConnector(connector : BiFunction<Server, HttpConfiguration, Connector>){
        connectors.add(connector)
    }
}
