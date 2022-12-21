package io.javalin.config

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory
import java.util.function.Consumer
import java.util.function.Supplier

class JettyConfig(private val pvt: PrivateConfig) {
    //@formatter:off
    @JvmField val multipartConfig = MultipartConfig()
    //@formatter:on

    fun server(server: Supplier<Server?>) {
        pvt.server = server.get()
    }

    fun contextHandlerConfig(consumer: Consumer<ServletContextHandler>) {
        pvt.servletContextHandlerConsumer = consumer
    }

    fun sessionHandler(sessionHandlerSupplier: Supplier<SessionHandler>) {
        pvt.sessionHandler = sessionHandlerSupplier.get()
    }

    fun wsFactoryConfig(wsFactoryConfig: Consumer<JettyWebSocketServletFactory>) {
        pvt.wsFactoryConfig = wsFactoryConfig
    }

}
