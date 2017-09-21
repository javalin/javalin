/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty

import io.javalin.core.JavalinServlet
import io.javalin.embeddedserver.EmbeddedServer
import io.javalin.embeddedserver.jetty.websocket.CustomWebSocketCreator
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.slf4j.LoggerFactory
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class EmbeddedJettyServer(private val server: Server, private val javalinServlet: JavalinServlet) : EmbeddedServer {

    private val log = LoggerFactory.getLogger(EmbeddedServer::class.java)

    override fun start(port: Int): Int {

        val httpHandler = object : SessionHandler() {
            override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                if (request.isWebSocket()) return // don't touch websocket requests
                javalinServlet.service(request.apply {
                    setAttribute("jetty-target", target)
                    setAttribute("jetty-request", jettyRequest)
                }, response)
                jettyRequest.isHandled = true
            }
        }

        val webSocketHandler = ServletContextHandler()
        javalinServlet.wsHandlers.forEach { path, handler ->
            webSocketHandler.addServlet(ServletHolder(object : WebSocketServlet() {
                override fun configure(factory: WebSocketServletFactory) {
                    val h = if (handler is Class<*>) handler.newInstance() else handler;
                    factory.creator = CustomWebSocketCreator(h)
                }
            }), path)
        }

        server.apply {
            handler = HandlerList(httpHandler, webSocketHandler)
            connectors = connectors.takeIf { it.isNotEmpty() } ?: arrayOf(ServerConnector(server).apply {
                this.port = port
            })
        }.start()

        log.info("Jetty is listening on: " + server.connectors.map { (if (it.protocols.contains("ssl")) "https" else "http") + "://localhost:" + (it as ServerConnector).localPort })

        return (server.connectors[0] as ServerConnector).localPort
    }

    override fun stop() {
        server.stop()
        server.join()
    }

    override fun activeThreadCount(): Int = server.threadPool.threads - server.threadPool.idleThreads
    override fun attribute(key: String): Any = server.getAttribute(key)

}

fun HttpServletRequest.isWebSocket(): Boolean = this.getHeader("Sec-WebSocket-Key") != null
