/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Javalin
import io.javalin.http.JavalinServlet
import io.javalin.websocket.JavalinWsServlet
import io.javalin.websocket.isWebSocket
import org.eclipse.jetty.server.*
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.handler.HandlerList
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.thread.QueuedThreadPool
import java.net.BindException
import java.util.function.Supplier
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinServer(val config: JavalinConfig) {

    var serverPort = 7000

    fun server(): Server {
        config.inner.server = config.inner.server ?: JettyUtil.getOrDefault(config.inner.server)
        return config.inner.server!!
    }

    var started = false

    @Throws(BindException::class)
    fun start(javalinServlet: JavalinServlet, javalinWsServlet: JavalinWsServlet) {

        config.inner.sessionHandler = config.inner.sessionHandler ?: defaultSessionHandler()
        val nullParent = null // javalin handlers are orphans

        val httpHandler = object : ServletContextHandler(nullParent, config.contextPath, SESSIONS) {
            override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                if (request.isWebSocket()) return // don't touch websocket requests
                request.setAttribute("jetty-target", target) // used in JettyResourceHandler
                request.setAttribute("jetty-request", jettyRequest)
                nextHandle(target, jettyRequest, request, response)
            }
        }.apply {
            this.sessionHandler = config.inner.sessionHandler
            config.inner.servletContextHandlerConsumer?.accept(this)
            addServlet(ServletHolder(javalinServlet), "/*")
        }

        val webSocketHandler = ServletContextHandler(nullParent, javalinWsServlet.config.wsContextPath, SESSIONS).apply {
            addServlet(ServletHolder(javalinWsServlet), "/*")
        }

        server().apply {
            handler = attachJavalinHandlers(server.handler, HandlerList(httpHandler, webSocketHandler))
            connectors = connectors.takeIf { it.isNotEmpty() } ?: arrayOf(ServerConnector(server).apply {
                this.port = serverPort
            })
        }.start()

        server().connectors.filterIsInstance<ServerConnector>().forEach {
            Javalin.log.info("Listening on ${it.protocol}://${it.host ?: "localhost"}:${it.localPort}${config.contextPath}")
        }

        server().connectors.filter { it !is ServerConnector }.forEach {
            Javalin.log.info("Binding to: $it")
        }

        JettyUtil.reEnableJettyLogger()
        serverPort = (server().connectors[0] as? ServerConnector)?.localPort ?: -1
    }

    private fun defaultSessionHandler() = SessionHandler().apply { httpOnly = true }

    private val ServerConnector.protocol get() = if (protocols.contains("ssl")) "https" else "http"

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

}

object JettyUtil {

    private var defaultLogger: org.eclipse.jetty.util.log.Logger? = null

    @JvmStatic
    fun getOrDefault(server: Server?) = server ?: Server(QueuedThreadPool(250, 8, 60_000)).apply {
        addBean(LowResourceMonitor(this))
        insertHandler(StatisticsHandler())
        setAttribute("is-default-server", true)
    }

    @JvmStatic
    fun disableJettyLogger() {
        defaultLogger = defaultLogger ?: org.eclipse.jetty.util.log.Log.getLog()
        org.eclipse.jetty.util.log.Log.setLog(NoopLogger())
    }

    fun reEnableJettyLogger() = org.eclipse.jetty.util.log.Log.setLog(defaultLogger)

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

