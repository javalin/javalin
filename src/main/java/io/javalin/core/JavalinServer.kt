/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Javalin
import io.javalin.core.util.Header
import io.javalin.websocket.JavalinWsServlet
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.thread.QueuedThreadPool
import java.io.ByteArrayInputStream
import java.net.BindException
import java.util.function.Supplier
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinServer {

    val config = JavalinServerConfig()
    val server = config.server

    private val jettyDefaultLogger = org.eclipse.jetty.util.log.Log.getLog()

    init {
        disableJettyLogger()
    }

    var started = false

    @Throws(BindException::class)
    fun start(javalinServlet: JavalinServlet, javalinWsServlet: JavalinWsServlet) {

        config.server = config.server ?: defaultServer()
        config.sessionHandler = config.sessionHandler ?: defaultSessionHandler()
        val nullParent = null // javalin handlers are orphans

        val httpHandler = object : ServletContextHandler(nullParent, config.contextPath, SESSIONS) {
            override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                if (request.isWebSocket()) return // don't touch websocket requests
                try {
                    request.setAttribute("jetty-target", target)
                    request.setAttribute("jetty-request", jettyRequest)
                    javalinServlet.service(request, response)
                } catch (t: Throwable) {
                    response.status = 500
                    Javalin.log.error("Exception occurred while servicing http-request", t)
                }
                jettyRequest.isHandled = true
            }
        }.apply {
            this.sessionHandler = config.sessionHandler
        }

        val webSocketHandler = ServletContextHandler(nullParent, config.contextPath).apply {
            addServlet(ServletHolder(javalinWsServlet), "/*")
        }

        val notFoundHandler = object : SessionHandler() {
            override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                val msg = "Not found. Request is below context-path (context-path: '${config.contextPath}')"
                response.status = 404
                ByteArrayInputStream(msg.toByteArray()).copyTo(response.outputStream)
                response.outputStream.close()
                Javalin.log.warn("Received a request below context-path (context-path: '${config.contextPath}'). Returned 404.")
            }
        }

        config.server!!.apply {
            handler = attachJavalinHandlers(server.handler, HandlerList(httpHandler, webSocketHandler, notFoundHandler))
            connectors = connectors.takeIf { it.isNotEmpty() } ?: arrayOf(ServerConnector(server).apply {
                this.port = config.port
            })
        }.start()

        config.server!!.connectors.filterIsInstance<ServerConnector>().forEach {
            Javalin.log.info("Listening on ${it.protocol}://${it.host ?: "localhost"}:${it.localPort}${config.contextPath}")
        }

        config.server!!.connectors.filter { it !is ServerConnector }.forEach {
            Javalin.log.info("Binding to: $it")
        }

        reEnableJettyLogger()
        started = true
        config.port = (config.server!!.connectors[0] as? ServerConnector)?.localPort ?: -1
    }

    private fun disableJettyLogger() = org.eclipse.jetty.util.log.Log.setLog(NoopLogger()) // disable logger before server creation
    private fun reEnableJettyLogger() = org.eclipse.jetty.util.log.Log.setLog(jettyDefaultLogger)

    private fun defaultServer() = Server(QueuedThreadPool(250, 8, 60_000)).apply {
        server.addBean(LowResourceMonitor(this))
        server.insertHandler(StatisticsHandler())
    }

    private fun defaultSessionHandler() = SessionHandler().apply { httpOnly = true }

    private val ServerConnector.protocol get() = if (this.protocols.contains("ssl")) "https" else "http"

    private fun attachJavalinHandlers(userHandler: Handler?, javalinHandlers: HandlerList) = when (userHandler) {
        null -> HandlerWrapper().apply { handler = javalinHandlers } // no custom Handler set, wrap Javalin handlers in a HandlerWrapper
        is HandlerCollection -> userHandler.apply { addHandler(javalinHandlers) } // user is using a HandlerCollection, add Javalin handlers to it
        is HandlerWrapper -> userHandler.apply {
            (unwrap(this) as? HandlerCollection)?.addHandler(javalinHandlers) // if HandlerWrapper unwraps as HandlerCollection, add Javalin handlers
            (unwrap(this) as? HandlerWrapper)?.handler = javalinHandlers // if HandlerWrapper unwraps as HandlerWrapper, add Javalin last
        }
        else -> throw IllegalStateException("Server has unidentified handler attached to it")
    }

    private fun unwrap(userHandler: HandlerWrapper): Handler = when (userHandler.handler) {
        null -> userHandler // current HandlerWrapper is last element, return the HandlerWrapper
        is HandlerCollection -> userHandler.handler // HandlerWrapper wraps HandlerCollection, return HandlerCollection
        is HandlerWrapper -> unwrap(userHandler.handler as HandlerWrapper) // HandlerWrapper wraps another HandlerWrapper, recursive call required
        else -> throw IllegalStateException("Cannot insert Javalin handlers into a Handler that is not a HandlerCollection or HandlerWrapper")
    }

    private fun HttpServletRequest.isWebSocket(): Boolean = this.getHeader(Header.SEC_WEBSOCKET_KEY) != null
}

object JettyUtil {

    @JvmStatic
    fun getSessionHandler(sessionHandlerSupplier: Supplier<SessionHandler>): SessionHandler {
        val sessionHandler = sessionHandlerSupplier.get()
        try {
            sessionHandler.sessionCache?.sessionDataStore?.exists("id-that-does-not-exist")
        } catch (e: Exception) {
            // TODO: This should throw... Find a way to check this that doesn't fail for valid SessionHandlers.
            Javalin.log.warn("Failed to look up ID in sessionDataStore. SessionHandler might be misconfigured.")
        }
        return sessionHandler
    }

}

class NoopLogger : org.eclipse.jetty.util.log.Logger {
    override fun getName() = "noop"
    override fun getLogger(name: String) = this
    override fun setDebugEnabled(enabled: Boolean) {}
    override fun isDebugEnabled() = false
    override fun ignore(ignored: Throwable) {}
    override fun warn(msg: String, vararg args: Any) {}
    override fun warn(thrown: Throwable) {}
    override fun warn(msg: String, thrown: Throwable) {}
    override fun info(msg: String, vararg args: Any) {}
    override fun info(thrown: Throwable) {}
    override fun info(msg: String, thrown: Throwable) {}
    override fun debug(msg: String, vararg args: Any) {}
    override fun debug(s: String, l: Long) {}
    override fun debug(thrown: Throwable) {}
    override fun debug(msg: String, thrown: Throwable) {}
}

