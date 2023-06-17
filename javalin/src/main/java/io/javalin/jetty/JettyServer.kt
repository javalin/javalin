/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.jetty

import io.javalin.config.JavalinConfig
import io.javalin.http.ContentType
import io.javalin.util.ConcurrencyUtil
import io.javalin.util.JavalinLogger
import io.javalin.util.Util
import io.javalin.util.Util.logJavalinBanner
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.http.HttpCookie
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.http.UriCompliance
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
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
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer
import java.net.BindException

class JettyServer(val cfg: JavalinConfig) {

    init {
        MimeTypes.getInferredEncodings()[ContentType.PLAIN] = Charsets.UTF_8.name() // set default encoding for text/plain
        Thread {
            Thread.sleep(5000)
            if (!this.started) {
                JavalinLogger.startup("It looks like you created a Javalin instance, but you never started it.")
                JavalinLogger.startup("Try: Javalin app = Javalin.create().start();")
                JavalinLogger.startup("For more help, visit https://javalin.io/documentation#server-setup")
            }
        }.start()
    }

    @JvmField
    var started = false

    @JvmField
    var serverPort = 8080

    @JvmField
    var serverHost: String? = null

    fun server(): Server {
        cfg.pvt.server = cfg.pvt.server ?: defaultServer()
        return cfg.pvt.server!!
    }

    @Throws(BindException::class)
    fun start(wsAndHttpServlet: JavalinJettyServlet) {
        val wsAndHttpHandler = createServletContextHandler()
        wsAndHttpHandler.contextPath = Util.normalizeContextPath(cfg.routing.contextPath)
        wsAndHttpHandler.sessionHandler = cfg.pvt.sessionHandler ?: defaultSessionHandler()
        wsAndHttpHandler.addServlet(ServletHolder(wsAndHttpServlet), "/*")
        cfg.pvt.servletContextHandlerConsumer?.accept(wsAndHttpHandler)

        server().apply {
            handler = if (handler == null) wsAndHttpHandler else handler.attachHandler(wsAndHttpHandler)
            if (connectors.isEmpty()) { // user has not added their own connectors, we add a single HTTP connector
                connectors = arrayOf(defaultConnector(this))
            }
        }.start() // start Jetty server

        logJavalinBanner(cfg.showJavalinBanner)

        (cfg.pvt.resourceHandler as? JettyResourceHandler)?.init() // we want to init this here to get logs in order

        server().connectors.filterIsInstance<ServerConnector>().forEach {
            JavalinLogger.startup("Listening on ${it.protocol}://${it.host ?: "localhost"}:${it.localPort}${cfg.routing.contextPath}")
        }

        server().connectors.filter { it !is ServerConnector }.forEach {
            JavalinLogger.startup("Binding to: $it")
        }

        serverPort = (server().connectors[0] as? ServerConnector)?.localPort ?: -1
    }

    private fun createServletContextHandler(): ServletContextHandler {
        val contextHandler = object : ServletContextHandler(SESSIONS) {
            override fun doHandle(target: String, jettyRequest: Request, request: HttpServletRequest, response: HttpServletResponse) {
                nextHandle(target, jettyRequest, request, response)
            }
        }
        JettyWebSocketServletContainerInitializer.configure(contextHandler, null) // enable WebSocket support
        return contextHandler
    }

    private fun defaultConnector(server: Server): ServerConnector {
        val httpConfiguration = defaultHttpConfiguration()
        cfg.pvt.httpConfigurationConfig?.accept(httpConfiguration) // apply the custom http configuration if we have one
        return ServerConnector(server, HttpConnectionFactory(httpConfiguration)).apply {
            this.port = serverPort
            this.host = serverHost
        }
    }

    private fun defaultSessionHandler() = SessionHandler().apply {
        httpOnly = true
        sameSite = HttpCookie.SameSite.LAX
    }

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

    companion object {
        fun defaultThreadPool() = ConcurrencyUtil.jettyThreadPool("JettyServerThreadPool", 8, 250)

        fun defaultServer() = Server(defaultThreadPool()).apply {
            addBean(LowResourceMonitor(this))
            insertHandler(StatisticsHandler())
            setAttribute("is-default-server", true)
        }

        // UriCompliance.RFC3986 makes Jetty accept ambiguous values in path, so Javalin can handle them
        // This is required to support ignoreTrailingSlashes, because Jetty 11 will refuse requests with doubled slashes
        fun defaultHttpConfiguration() = HttpConfiguration().apply {
            uriCompliance = UriCompliance.RFC3986
            sendServerVersion = false
        }
    }

}
