package io.javalin.config

import org.eclipse.jetty.ee9.servlet.ServletContextHandler
import org.eclipse.jetty.ee9.websocket.server.JettyWebSocketServletFactory
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.Server
import java.util.function.BiFunction
import java.util.function.Consumer

class JettyInternalConfig {
    // @formatter:off
    @JvmField var server: Server? = null
    @JvmField var serverConsumers: MutableList<Consumer<Server>> = mutableListOf()
    @JvmField var httpConfigurationConfigs: MutableList<Consumer<HttpConfiguration>> = mutableListOf()
    @JvmField var servletContextHandlerConsumers: MutableList<Consumer<ServletContextHandler>> = mutableListOf()
    @JvmField var wsFactoryConfigs: MutableList<Consumer<JettyWebSocketServletFactory>> = mutableListOf()
    @JvmField var connectors: MutableList<BiFunction<Server, HttpConfiguration, Connector>> = mutableListOf()
    // @formatter:on
}
