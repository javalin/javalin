/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty

import io.javalin.core.JavalinServlet
import io.javalin.embeddedserver.EmbeddedServer
import io.javalin.embeddedserver.jetty.websocket.CustomWebSocketCreator
import org.eclipse.jetty.server.Handler
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
                          private val interceptors: List<Handler>) : EmbeddedServer {

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
            // for us to build a proper chain, all the interceptors must be HandlerWrapper instances
            // The out-of-the-box handlers (i.e. StatisticsHandler, RequestLogHandler, etc.) are all HandlerWrapper
            // derivatives so we return them as is.
            val wrappedInterceptors =
                interceptors.map {
                    if (it is HandlerWrapper)
                        it
                    else {
                        val w = HandlerWrapper()
                        w.handler = it
                        w
                    }
                }

            // this Handler will always be used...
            val always = ShortCircuitHandlerList(httpHandler, webSocketHandler, notFoundHandler)

            handler =
                if (wrappedInterceptors.isNotEmpty()) {
                    val folded = wrappedInterceptors.drop(1).fold(wrappedInterceptors.first()) {
                        acc, wrapper -> acc.insertHandler(wrapper); wrapper
                    }
                    folded.handler = always
                    folded
                } else {
                    always
                }

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

// HandlerList will dispatch the request even if it has already been handled. So let's not do that
// Reason being - if any of the interceptors handle the request, no reason to pass it on.
private class ShortCircuitHandlerList(vararg handlers: Handler) : HandlerList(*handlers) {
    override fun handle(target: String?,
                        baseRequest: Request?,
                        request: HttpServletRequest?,
                        response: HttpServletResponse?) {
        if (baseRequest != null && baseRequest.isHandled) {
            return
        }

        super.handle(target, baseRequest, request, response)
    }
}
