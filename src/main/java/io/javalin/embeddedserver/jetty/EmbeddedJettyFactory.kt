/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty

import io.javalin.core.ErrorMapper
import io.javalin.core.ExceptionMapper
import io.javalin.core.JavalinServlet
import io.javalin.core.PathMatcher
import io.javalin.embeddedserver.EmbeddedServer
import io.javalin.embeddedserver.EmbeddedServerFactory
import io.javalin.embeddedserver.StaticFileConfig
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.thread.QueuedThreadPool
import java.util.concurrent.TimeUnit


class EmbeddedJettyFactory : EmbeddedServerFactory {

    private var server: Server;

    constructor() {
        this.server = Server(QueuedThreadPool(200, 8, 60000))
    }

    constructor(jettyServer: () -> Server) {
        this.server = jettyServer.invoke()
    }

    override fun create(pathMatcher: PathMatcher, exceptionMapper: ExceptionMapper, errorMapper: ErrorMapper, staticFileConfig: StaticFileConfig?): EmbeddedServer {
        val resourceHandler = JettyResourceHandler(staticFileConfig)
        val javalinServlet = JavalinServlet(pathMatcher, exceptionMapper, errorMapper, resourceHandler)
        return EmbeddedJettyServer(server, JettyHandler(javalinServlet))
    }

    companion object {
        fun defaultConnector(server: Server, host: String, port: Int): ServerConnector {
            val connector = ServerConnector(server)
            connector.idleTimeout = TimeUnit.HOURS.toMillis(1)
            connector.soLingerTime = -1
            connector.host = host
            connector.port = port
            return connector
        }
    }

}
