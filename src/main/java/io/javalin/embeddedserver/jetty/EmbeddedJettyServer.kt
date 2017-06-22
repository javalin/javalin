/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty

import io.javalin.embeddedserver.EmbeddedServer
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.session.SessionHandler
import org.slf4j.LoggerFactory

class EmbeddedJettyServer(private val server: Server, private val javalinHandler: SessionHandler) : EmbeddedServer {

    private val log = LoggerFactory.getLogger(EmbeddedServer::class.java)

    override fun start(host: String, port: Int): Int {

        server.apply {
            handler = javalinHandler
            connectors = if (connectors.isNotEmpty()) connectors else arrayOf(ServerConnector(server).apply {
                this.host = host
                this.port = port
            })
        }.start()

        log.info("Jetty is listening on: " + server.connectors.map { (if (it.protocols.contains("ssl")) "https" else "http") + "://localhost:" + (it as ServerConnector).localPort })

        return (server.connectors[0] as ServerConnector).localPort
    }

    override fun join() = server.join()
    override fun stop() = server.stop()
    override fun activeThreadCount(): Int = server.threadPool.threads - server.threadPool.idleThreads
    override fun attribute(key: String): Any = server.getAttribute(key)

}
