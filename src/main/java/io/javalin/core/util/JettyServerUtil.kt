/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.util

import io.javalin.Javalin
import io.javalin.core.JavalinServlet
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
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.net.BindException
import java.util.*
import java.util.function.Supplier
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

object JettyServerUtil {

    private val jettyDefaultLogger = org.eclipse.jetty.util.log.Log.getLog()
    private val log = LoggerFactory.getLogger(Javalin::class.java) // let's pretend

    @JvmStatic
    fun reEnableJettyLogger() = org.eclipse.jetty.util.log.Log.setLog(jettyDefaultLogger)

    @JvmStatic
    fun defaultServer(): Server {
        org.eclipse.jetty.util.log.Log.setLog(io.javalin.core.util.NoopLogger()) // disable logger before server creation
        return Server(QueuedThreadPool(250, 8, 60_000)).apply {
            server.addBean(LowResourceMonitor(this))
            server.insertHandler(StatisticsHandler())
        }
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
            javalinWsServlet: JavalinWsServlet
    ): Int {

        val nullParent = null // javalin handlers are orphans

        val httpHandler = object : ServletContextHandler(nullParent, contextPath, SESSIONS) {
            override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                if (request.isWebSocket()) return // don't touch websocket requests
                try {
                    request.setAttribute("jetty-target", target)
                    request.setAttribute("jetty-request", jettyRequest)
                    javalinServlet.service(request, response)
                } catch (t: Throwable) {
                    response.status = 500
                    log.error("Exception occurred while servicing http-request", t)
                }
                jettyRequest.isHandled = true
            }
        }.apply {
            this.sessionHandler = sessionHandler
        }

        val webSocketHandler = ServletContextHandler(nullParent, contextPath).apply {
            addServlet(ServletHolder(javalinWsServlet), "/*")
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
            handler = attachJavalinHandlers(server.handler, HandlerList(httpHandler, webSocketHandler, notFoundHandler))
            connectors = connectors.takeIf { it.isNotEmpty() } ?: arrayOf(ServerConnector(server).apply {
                this.port = port
            })
        }.start()

        server.connectors.filterIsInstance<ServerConnector>().forEach {
            log.info("Listening on ${it.protocol}://${it.host ?: "localhost"}:${it.localPort}$contextPath")
        }

        server.connectors.filter { it !is ServerConnector }.forEach {
            log.info("Binding to: $it")
        }

        return (server.connectors[0] as? ServerConnector)?.localPort ?: -1
    }

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

    var noJettyStarted = true
    fun printHelpfulMessageIfNoServerHasBeenStartedAfterOneSecond() {
        // per instance checks are not considered necessary
        // this helper is not intended for people with more than one instance
        Thread {
            Thread.sleep(1000)
            if (noJettyStarted) {
                log.info("It looks like you created a Javalin instance, but you never started it.")
                log.info("Try: Javalin app = Javalin.create().start();")
                log.info("For more help, visit https://javalin.io/documentation#starting-and-stopping")
            }
        }.start()
    }

    fun getValidSessionHandlerOrThrow(sessionHandlerSupplier: Supplier<SessionHandler>): SessionHandler {
        val uuid = UUID.randomUUID().toString()
        val sessionHandler = sessionHandlerSupplier.get()
        return if (sessionHandler.sessionCache == null)
            sessionHandler
        else try {
            sessionHandler.isIdInUse(uuid)
            sessionHandler
        } catch (e: Exception) {
            throw IllegalStateException("Could not look up dummy session ID in store. Misconfigured session handler", e)
        }
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
