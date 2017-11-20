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
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class EmbeddedJettyServer(private val server: Server,
                          private val javalinServlet: JavalinServlet,
                          private val decorator: HandlerWrapper? = null) : EmbeddedServer {

    private val log = LoggerFactory.getLogger(EmbeddedServer::class.java)

    val parent = null // javalin handlers are orphans

    override fun start(port: Int): Int {

        val httpHandler = object : ServletContextHandler(parent, javalinServlet.contextPath, SESSIONS) {
            override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                if (request.isWebSocket()) return // don't touch websocket requests
                try {
                    request.setAttribute("jetty-target", target)
                    request.setAttribute("jetty-request", jettyRequest)
                    javalinServlet.service(request, response)
                } catch (e: Exception) {
                    log.error("Exception occurred while servicing http-request", e)
                }
                jettyRequest.isHandled = true
            }
        }

        val webSocketHandler = ServletContextHandler(parent, javalinServlet.contextPath).apply {
            javalinServlet.wsHandlers.forEach { path, handler ->
                addServlet(ServletHolder(object : WebSocketServlet() {
                    override fun configure(factory: WebSocketServletFactory) {
                        val h = if (handler is Class<*>) handler.newInstance() else handler;
                        factory.creator = CustomWebSocketCreator(h)
                    }
                }), path)
            }
        }

        val notFoundHandler = object : SessionHandler() {
            override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                val msg = "Not found. Request is below context-path (context-path: '${javalinServlet.contextPath}')"
                response.status = 404
                ByteArrayInputStream(msg.toByteArray()).copyTo(response.outputStream)
                response.outputStream.close()
                log.warn("Received a request below context-path (context-path: '${javalinServlet.contextPath}'). Returned 404.")
            }
        }

        server.apply {
            val always = HandlerList(httpHandler, webSocketHandler, notFoundHandler)

            handler =
                decorator?.let {
                    // we might be a chain. So we have to find the end of the chain...
                    var tail = it

                    while (tail.handler != null && tail.handler is HandlerWrapper) {
                        tail = tail.handler as HandlerWrapper
                    }
                    if (tail.handler != null) {
                        throw IllegalArgumentException(
                            "tail of the Decorator chain is not null and is not a HandlerWrapper")
                    }
                    tail.handler = always

                    // but make sure to return the head of the chain..
                    it
                } ?: always

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
