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

class EmbeddedJettyFactory : EmbeddedServerFactory {

    private val server: Server;

    constructor() {
        this.server = Server(QueuedThreadPool(200, 8, 60000))
    }

    constructor(jettyServer: () -> Server) {
        this.server = jettyServer.invoke()
    }

    override fun create(javalinServlet: JavalinServlet, staticFileConfig: StaticFileConfig?): EmbeddedServer {
        return EmbeddedJettyServer(server, javalinServlet.apply { staticResourceHandler = JettyResourceHandler(staticFileConfig) })
    }

}
