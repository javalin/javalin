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
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.QueuedThreadPool

class EmbeddedJettyFactory(jettyServer: () -> Server = { Server(QueuedThreadPool(250, 8, 60000)) }) : EmbeddedServerFactory {
    private val server = jettyServer()
    private val interceptors: MutableList<Handler> = mutableListOf()

    @JvmOverloads constructor(interceptors: List<Handler>,
                              jettyServer: () -> Server = { Server(QueuedThreadPool(250, 8, 60000)) })
        : this(jettyServer) {
        this.interceptors.addAll(interceptors)
    }

    override fun create(javalinServlet: JavalinServlet, staticFileConfig: StaticFileConfig?): EmbeddedServer {
        return EmbeddedJettyServer(
            server,
            javalinServlet.apply { staticResourceHandler = JettyResourceHandler(staticFileConfig) },
            interceptors)
    }
}
