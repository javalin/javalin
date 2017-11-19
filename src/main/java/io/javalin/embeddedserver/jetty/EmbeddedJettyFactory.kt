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
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.util.thread.QueuedThreadPool

class EmbeddedJettyFactory constructor(jettyServer: () -> Server = { Server(QueuedThreadPool(250, 8, 60000)) }) : EmbeddedServerFactory {
    private val server = jettyServer()
    private var statisticsHandler: StatisticsHandler? = null

    @JvmOverloads constructor(statisticsHandler: StatisticsHandler?,
                              jettyServer: () -> Server = { Server(QueuedThreadPool(250, 8, 60000)) }) : this(jettyServer) {
        this.statisticsHandler = statisticsHandler
    }

    override fun create(javalinServlet: JavalinServlet, staticFileConfig: StaticFileConfig?): EmbeddedServer {
        return EmbeddedJettyServer(
            server,
            javalinServlet.apply { staticResourceHandler = JettyResourceHandler(staticFileConfig) },
            statisticsHandler)
    }
}
