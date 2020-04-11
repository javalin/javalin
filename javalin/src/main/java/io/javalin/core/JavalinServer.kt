/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Javalin
import io.javalin.websocket.JavalinWsServlet
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.LowResourceMonitor
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.thread.QueuedThreadPool
import java.net.BindException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinServer(val config: JavalinConfig) {

    var serverPort = 7000
    var serverHost: String? = null

    fun server(): Server {
        config.inner.server = config.inner.server ?: JettyUtil.getOrDefault(config.inner.server)
        return config.inner.server!!
    }

    var started = false

    @Throws(BindException::class)
    fun start(wsAndHttpServlet: JavalinWsServlet) {

        config.inner.sessionHandler = config.inner.sessionHandler ?: defaultSessionHandler()
        val nullParent = null // javalin handlers are orphans

        val wsAndHttpHandler = object : ServletContextHandler(nullParent, config.contextPath, SESSIONS) {
            override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                request.setAttribute("jetty-target", target) // used in JettyResourceHandler
                request.setAttribute("jetty-request", jettyRequest)
                nextHandle(target, jettyRequest, request, response)
            }
        }.apply {
            this.sessionHandler = config.inner.sessionHandler
            config.inner.servletContextHandlerConsumer?.accept(this)
            addServlet(ServletHolder(wsAndHttpServlet), "/*")
        }

        server().apply {
            handler = if (server.handler == null) wsAndHttpHandler else server.handler.attachHandler(wsAndHttpHandler)
            connectors = connectors.takeIf { it.isNotEmpty() } ?: arrayOf(ServerConnector(server).apply {
                this.port = serverPort
                this.host = serverHost
            })
        }.start()

        server().connectors.filterIsInstance<ServerConnector>().forEach {
            Javalin.log?.info("Listening on ${it.protocol}://${it.host ?: "localhost"}:${it.localPort}${config.contextPath}")
        }

        server().connectors.filter { it !is ServerConnector }.forEach {
            Javalin.log?.info("Binding to: $it")
        }

        JettyUtil.reEnableJettyLogger()
        serverPort = (server().connectors[0] as? ServerConnector)?.localPort ?: -1
    }

    private fun defaultSessionHandler() = SessionHandler().apply { httpOnly = true }

    private val ServerConnector.protocol get() = if (protocols.contains("ssl")) "https" else "http"

    private fun Handler.attachHandler(servletContextHandler: ServletContextHandler) = when (this) {
        is HandlerCollection -> this.apply { addHandler(servletContextHandler) } // user is using a HandlerCollection, add Javalin handler to it
        is HandlerWrapper -> this.apply {
            (this.unwrap() as? HandlerCollection)?.addHandler(servletContextHandler) // if HandlerWrapper unwraps as HandlerCollection, add Javalin handler
            (this.unwrap() as? HandlerWrapper)?.handler = servletContextHandler // if HandlerWrapper unwraps as HandlerWrapper, add Javalin last
        }
        else -> throw IllegalStateException("Server has unsupported Handler attached to it (must be HandlerCollection or HandlerWrapper)")
    }

    private fun HandlerWrapper.unwrap(): Handler = when (this.handler) {
        null -> this // current HandlerWrapper is last element, return the HandlerWrapper itself
        is HandlerCollection -> this.handler // HandlerWrapper wraps HandlerCollection, return HandlerCollection
        is HandlerWrapper -> (this.handler as HandlerWrapper).unwrap() // HandlerWrapper wraps another HandlerWrapper, recursive call required
        else -> throw IllegalStateException("HandlerWrapper has unsupported Handler type (must be HandlerCollection or HandlerWrapper")
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

