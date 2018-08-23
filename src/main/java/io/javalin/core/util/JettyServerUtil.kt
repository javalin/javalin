/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.core.JavalinServlet
import io.javalin.websocket.WsPathMatcher
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import org.slf4j.Logger
import java.io.ByteArrayInputStream
import java.net.BindException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object JettyServerUtil {

    @JvmStatic
    fun defaultServer() = Server(QueuedThreadPool(250, 8, 60_000)).apply {
        server.addBean(LowResourceMonitor(this))
        server.insertHandler(StatisticsHandler())
    }

    @JvmStatic
    fun defaultSessionHandler() = SessionHandler().apply { httpOnly = true }

    @JvmStatic
    @Throws(BindException::class)
    fun initialize(
            server: Server,
            sessionHandler: SessionHandler,
            port: Int,
            contextPath: String,
            javalinServlet: JavalinServlet,
            wsPathMatcher: WsPathMatcher,
            log: Logger
    ): Int {

        val parent = null // javalin handlers are orphans

        val httpHandler = object : ServletContextHandler(parent, contextPath, SESSIONS) {
            override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                if (request.isWebSocket()) return // don't touch websocket requests
                try {
                    request.setAttribute("jetty-target", target)
                    request.setAttribute("jetty-request", jettyRequest)
                    javalinServlet.service(request, response)
                } catch (e: Exception) {
                    response.status = 500
                    log.error("Exception occurred while servicing http-request", e)
                }
                jettyRequest.isHandled = true
            }
        }.apply {
            this.sessionHandler = sessionHandler
        }

        val webSocketHandler = ServletContextHandler(parent, contextPath).apply {
            addServlet(ServletHolder(object : WebSocketServlet() {
                override fun configure(factory: WebSocketServletFactory) {
                    factory.creator = WebSocketCreator { req, res ->
                        wsPathMatcher.findEntry(req) ?: res.sendError(404, "WebSocket handler not found")
                        wsPathMatcher // this is a long-lived object handling multiple connections
                    }
                }
            }), "/*")
        }

        val notFoundHandler = object : SessionHandler() {
            override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                val msg = "Not found. Request is below context-path (context-path: '$contextPath')"
                response.status = 404
                ByteArrayInputStream(msg.toByteArray()).copyTo(response.outputStream)
                response.outputStream.close()
                log.warn("Received a request below context-path (context-path: '$contextPath'). Returned 404.")
            }
        }

        server.apply {
            handler = attachHandlersToTail(server.handler, HandlerList(httpHandler, webSocketHandler, notFoundHandler))
            connectors = connectors.takeIf { it.isNotEmpty() } ?: arrayOf(ServerConnector(server).apply {
                this.port = port
            })
        }.start()

        log.info("Jetty is listening on: " + server.connectors.map { it as ServerConnector; (if (it.protocols.contains("ssl")) "https" else "http") + "://${it.host ?: "localhost"}:${it.localPort}" })

        return (server.connectors[0] as ServerConnector).localPort
    }

    private fun attachHandlersToTail(userHandler: Handler?, handlerList: HandlerList): HandlerWrapper {
        val handlerWrapper = (userHandler ?: HandlerWrapper()) as HandlerWrapper
        HandlerWrapper().apply { handler = handlerList }.insertHandler(handlerWrapper)
        return handlerWrapper
    }

    private fun HttpServletRequest.isWebSocket(): Boolean = this.getHeader(Header.SEC_WEBSOCKET_KEY) != null
}
