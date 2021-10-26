/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.jetty

import io.javalin.core.JavalinConfig
import io.javalin.core.util.JavalinLogger
import io.javalin.core.util.Util
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import java.net.BindException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JettyServer(val config: JavalinConfig) {

    @JvmField
    var started = false
    var serverPort = -1
    var serverHost: String? = null

    fun server(): Server {
        config.inner.server = config.inner.server ?: JettyUtil.getOrDefault(config.inner.server)
        return config.inner.server!!
    }

    @Throws(BindException::class)
    fun start(wsAndHttpServlet: JavalinJettyServlet) {
        if (serverPort == -1 && config.inner.server == null) {
            serverPort = 8080
            JavalinLogger.startup("No port specified, starting on port $serverPort. Call start(port) to change ports.")
        }

        config.inner.sessionHandler = config.inner.sessionHandler ?: defaultSessionHandler()
        val nullParent = null // javalin handlers are orphans

        val wsAndHttpHandler = object : ServletContextHandler(nullParent, Util.normalizeContextPath(config.contextPath), SESSIONS) {
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
            handler = if (handler == null) wsAndHttpHandler else handler.attachHandler(wsAndHttpHandler)
            if (connectors.isEmpty()) { // user has not added their own connectors, we add a single HTTP connector
                connectors = arrayOf(defaultConnector(this))
            }
        }.start()

        server().connectors.filterIsInstance<ServerConnector>().forEach {
            JavalinLogger.startup("Listening on ${it.protocol}://${it.host ?: "localhost"}:${it.localPort}${config.contextPath}")
        }

        server().connectors.filter { it !is ServerConnector }.forEach {
            JavalinLogger.startup("Binding to: $it")
        }

        JettyUtil.reEnableJettyLogger()
        serverPort = (server().connectors[0] as? ServerConnector)?.localPort ?: -1
    }

    private fun defaultConnector(server: Server) = ServerConnector(server).apply {
        this.port = serverPort
        this.host = serverHost
        this.getConnectionFactories().forEach {
            if (it is HttpConnectionFactory) {
                it.getHttpConfiguration().setSendServerVersion(false)
            }
        }
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
