/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.jetty

import io.javalin.config.JavalinConfig
import io.javalin.event.EventManager
import io.javalin.event.JavalinEvent
import io.javalin.http.ContentType
import io.javalin.util.ConcurrencyUtil
import io.javalin.util.JavalinBindException
import io.javalin.util.JavalinException
import io.javalin.util.JavalinLogger
import io.javalin.util.Util
import io.javalin.util.Util.getPort
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
import kotlin.Exception
import kotlin.IllegalStateException
import kotlin.String
import kotlin.Throws
import kotlin.apply
import kotlin.arrayOf

class JettyServer(
    val cfg: JavalinConfig,
    private val wsAndHttpServlet: JavalinJettyServlet,
    private val eventManager: EventManager
) {

    init {
        MimeTypes.getInferredEncodings()[ContentType.PLAIN] = Charsets.UTF_8.name() // set default encoding for text/plain
        Thread {
            Thread.sleep(5000)
            if (!started) {
                JavalinLogger.startup("It looks like you created a Javalin instance, but you never started it.")
                JavalinLogger.startup("Try: Javalin app = Javalin.create().start();")
                JavalinLogger.startup("For more help, visit https://javalin.io/documentation#server-setup")
            }
        }.start()
    }

    //@formatter:off
    @JvmField var started = false
    @JvmField var serverPort = 8080
    @JvmField var serverHost: String? = null
    @JvmField var server = cfg.pvt.server
    //@formatter:on

    private val wsAndHttpHandler = createServletContextHandler().apply {
        contextPath = Util.normalizeContextPath(cfg.routing.contextPath)
        sessionHandler = cfg.pvt.sessionHandler
        addServlet(ServletHolder(wsAndHttpServlet), "/*")
        cfg.pvt.servletContextHandlerConsumer?.accept(this)
    }

    @Throws(JavalinException::class)
    fun start() {
        if (started) {
            throw JavalinException("Server already started - Javalin instances cannot be reused.")
        }
        started = true
        val startupTimer = System.currentTimeMillis()
        server.apply {
            handler = handler.attachHandler(wsAndHttpHandler)
            connectors = if (connectors.isEmpty()) arrayOf(defaultConnector(this)) else connectors
        }
        eventManager.fireEvent(JavalinEvent.SERVER_STARTING)
        try {
            JavalinLogger.startup("Starting Javalin ...")
            server.start()
            JavalinLogger.startup("Javalin started in " + (System.currentTimeMillis() - startupTimer) + "ms \\o/")
            eventManager.fireEvent(JavalinEvent.SERVER_STARTED)
        } catch (e: Exception) {
            JavalinLogger.error("Failed to start Javalin")
            eventManager.fireEvent(JavalinEvent.SERVER_START_FAILED)
            if (server.getAttribute("is-default-server") == true) {
                server.stop() // stop if server is default server; otherwise, the caller is responsible to stop
            }
            if (e.message != null && e.message!!.contains("Failed to bind to")) {
                throw JavalinBindException("Port already in use. Make sure no other process is using port " + getPort(e) + " and try again.", e)
            } else if (e.message != null && e.message!!.contains("Permission denied")) {
                throw JavalinBindException("Port 1-1023 require elevated privileges (process must be started by admin).", e)
            }
            throw JavalinException(e)
        }
        if (cfg.showJavalinBanner) JavalinLogger.startup(
            """|
               |       __                  ___           _____
               |      / /___ __   ______ _/ (_)___      / ___/
               | __  / / __ `/ | / / __ `/ / / __ \    / __ \
               |/ /_/ / /_/ /| |/ / /_/ / / / / / /   / /_/ /
               |\____/\__,_/ |___/\__,_/_/_/_/ /_/    \____/
               |
               |       https://javalin.io/documentation
               |""".trimMargin()
        )
        (cfg.pvt.resourceHandler as? JettyResourceHandler)?.init() // log resource handler info
        server.connectors.filterIsInstance<ServerConnector>().forEach {
            JavalinLogger.startup("Listening on ${it.baseUrl}")
        }
        server.connectors.filter { it !is ServerConnector }.forEach {
            JavalinLogger.startup("Binding to: $it")
        }
        serverPort = (server.connectors[0] as ServerConnector).localPort // there will always be at least one connector
        Util.logJavalinVersion()
    }

    fun stop() {
        JavalinLogger.info("Stopping Javalin ...")
        eventManager.fireEvent(JavalinEvent.SERVER_STOPPING)
        try {
            server.stop()
        } catch (e: Exception) {
            eventManager.fireEvent(JavalinEvent.SERVER_STOP_FAILED)
            JavalinLogger.error("Javalin failed to stop gracefully", e)
            throw JavalinException(e)
        }
        JavalinLogger.info("Javalin has stopped")
        eventManager.fireEvent(JavalinEvent.SERVER_STOPPED)
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

    private fun Handler?.attachHandler(servletContextHandler: ServletContextHandler) = when {
        this == null -> servletContextHandler // server has no handler, just use Javalin handler
        this is HandlerCollection -> this.apply { addHandler(servletContextHandler) } // user is using a HandlerCollection, add Javalin handler to it
        this is HandlerWrapper -> this.apply {
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

        fun defaultSessionHandler() = SessionHandler().apply {
            httpOnly = true
            sameSite = HttpCookie.SameSite.LAX
        }
    }

    private val ServerConnector.protocol get() = if (protocols.contains("ssl")) "https" else "http"
    private val ServerConnector.baseUrl get() = "${this.protocol}://${this.host ?: "localhost"}:${this.localPort}${cfg.routing.contextPath}"

}
