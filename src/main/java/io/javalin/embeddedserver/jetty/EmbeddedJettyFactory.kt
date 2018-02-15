/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty

import io.javalin.core.JavalinServlet
import io.javalin.embeddedserver.EmbeddedServer
import io.javalin.embeddedserver.EmbeddedServerFactory
import io.javalin.embeddedserver.StaticFileConfig
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.QueuedThreadPool

/**
 * Default Javalin [EmbeddedServerFactory] implementation. Allows customization of internal Jetty server instance.
 *
 * @see <a href="https://javalin.io/documentation#custom-server">Custom server in docs</a>
 */
class EmbeddedJettyFactory(jettyServer: () -> Server = { Server(QueuedThreadPool(250, 8, 60000)) }) : EmbeddedServerFactory {
    private val server = jettyServer()
    override fun create(javalinServlet: JavalinServlet, staticFileConfig: List<StaticFileConfig>): EmbeddedServer {
        return EmbeddedJettyServer(server, javalinServlet.apply { staticResourceHandler = JettyResourceHandler(staticFileConfig) })
    }
}
