package io.javalin.core.config

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory
import java.util.function.Consumer
import java.util.function.Supplier

class JettyConfig(private val inner: InnerConfig) {

    @JvmField
    var contextPath = "/"

    fun server(server: Supplier<Server?>) {
        inner.server = server.get()
    }

    fun contextHandlerConfig(consumer: Consumer<ServletContextHandler>) {
        inner.servletContextHandlerConsumer = consumer
    }

    fun sessionHandler(sessionHandlerSupplier: Supplier<SessionHandler>) {
        inner.sessionHandler = sessionHandlerSupplier.get()
    }

    fun wsFactoryConfig(wsFactoryConfig: Consumer<JettyWebSocketServletFactory>) {
        inner.wsFactoryConfig = wsFactoryConfig
    }

}
