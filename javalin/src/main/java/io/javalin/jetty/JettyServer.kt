/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.jetty

import io.javalin.config.JavalinConfig
import io.javalin.event.JavalinLifecycleEvent
import io.javalin.http.ContentType
import io.javalin.util.ConcurrencyUtil
import io.javalin.util.JavalinBindException
import io.javalin.util.JavalinException
import io.javalin.util.JavalinLogger
import io.javalin.util.Util
import io.javalin.util.Util.getPort
import org.eclipse.jetty.http.HttpCookie
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.http.UriCompliance
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.LowResourceMonitor
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.ee10.servlet.SessionHandler
import org.eclipse.jetty.ee10.servlet.ServletContextHandler
import org.eclipse.jetty.ee10.servlet.ServletContextHandler.SESSIONS
import org.eclipse.jetty.ee10.servlet.ServletHolder
import org.eclipse.jetty.util.thread.ThreadPool

class JettyServer(private val cfg: JavalinConfig) {

    init {
        MimeTypes.getInferredEncodings()[ContentType.PLAIN] = Charsets.UTF_8.name() // set default encoding for text/plain

        if (cfg.startupWatcherEnabled) {
            Thread {
                Thread.sleep(5000)
                if (!started) {
                    JavalinLogger.startup("It looks like you created a Javalin instance, but you never started it.")
                    JavalinLogger.startup("Try: Javalin app = Javalin.create().start();")
                    JavalinLogger.startup("For more help, visit https://javalin.io/documentation#server-setup")
                }
            }.start()
        }
    }

    fun threadPool() = cfg.jetty.threadPool ?: defaultThreadPool(cfg.useVirtualThreads).also { cfg.jetty.threadPool = it } // make sure config has access to the thread pool instance
    fun server() = cfg.pvt.jetty.server ?: defaultServer(threadPool()).also { cfg.pvt.jetty.server = it } // make sure config has access to the update server instance
    fun port() = (server().connectors[0] as ServerConnector).localPort

    private var started = false
    fun started() = started

    private val eventManager by lazy { cfg.pvt.eventManager }

    @Throws(JavalinException::class)
    fun start(host: String?, port: Int?) {
        Util.printHelpfulMessageIfLoggerIsMissing()
        if (started) {
            throw JavalinException("Server already started - Javalin instances cannot be reused.")
        }
        started = true
        val startupTimer = System.currentTimeMillis()
        server().apply {
            cfg.pvt.jetty.serverConsumers.forEach { it.accept(this) } // apply user config
            handler = handler.attachHandler(ServletContextHandler(SESSIONS).apply {
                val (initializer, servlet) = cfg.pvt.servlet.value
                if (initializer != null) this.addServletContainerInitializer(initializer)
                contextPath = Util.normalizeContextPath(cfg.router.contextPath)
                sessionHandler = defaultSessionHandler()
                addServlet(ServletHolder(servlet), "/*")
                cfg.pvt.jetty.servletContextHandlerConsumers.forEach { it.accept(this) } // apply user config
            })
            val httpConfiguration = defaultHttpConfiguration()
            cfg.pvt.jetty.httpConfigurationConfigs.forEach { it.accept(httpConfiguration) } // apply user config (before connectors)
            // use the jetty value, either the default or something the user has specified with the cfg.jetty.modifyHttpConfiguration option if there is no value set with the new api.
            if (cfg.http.responseBufferSize == null) {
                cfg.http.responseBufferSize = httpConfiguration.outputBufferSize
            }
            cfg.pvt.jetty.connectors.map { it.apply(this, httpConfiguration) }.forEach(this::addConnector) // add user connectors
            if (connectors.isEmpty()) { // add default connector if no connectors are specified
                connectors = arrayOf(ServerConnector(server, HttpConnectionFactory(httpConfiguration)).apply {
                    this.host = host ?: cfg.jetty.defaultHost
                    this.port = port ?: cfg.jetty.defaultPort
                })
            }
        }
        eventManager.fireEvent(JavalinLifecycleEvent.SERVER_STARTING)
        try {
            JavalinLogger.startup("Starting Javalin ...")
            server().start() // this will log a lot of stuff
        } catch (e: Exception) {
            JavalinLogger.error("Failed to start Javalin")
            eventManager.fireEvent(JavalinLifecycleEvent.SERVER_START_FAILED)
            if (server().getAttribute("is-default-server") == true) {
                server().stop() // stop if server is default server; otherwise, the caller is responsible to stop
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
        JavalinLogger.startup("Javalin started in " + (System.currentTimeMillis() - startupTimer) + "ms \\o/")
        (cfg.pvt.resourceHandler as? JettyResourceHandler)?.init() // log resource handler info
        server().connectors.filterIsInstance<ServerConnector>().forEach {
            JavalinLogger.startup("Listening on ${it.baseUrl}")
        }
        server().connectors.filter { it !is ServerConnector }.forEach {
            JavalinLogger.startup("Binding to: $it")
        }
        Util.logJavalinVersion()
        eventManager.fireEvent(JavalinLifecycleEvent.SERVER_STARTED)
    }

    fun stop() {
        JavalinLogger.info("Stopping Javalin ...")
        eventManager.fireEvent(JavalinLifecycleEvent.SERVER_STOPPING)
        try {
            server().stop()
        } catch (e: Exception) {
            eventManager.fireEvent(JavalinLifecycleEvent.SERVER_STOP_FAILED)
            JavalinLogger.error("Javalin failed to stop gracefully", e)
            throw JavalinException(e)
        }
        JavalinLogger.info("Javalin has stopped")
        eventManager.fireEvent(JavalinLifecycleEvent.SERVER_STOPPED)
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
        fun defaultThreadPool(useVirtualThreads: Boolean) = ConcurrencyUtil.jettyThreadPool("JettyServerThreadPool", 8, 250, useVirtualThreads)

        fun defaultServer(threadPool: ThreadPool) = Server(threadPool).apply {
            addBean(LowResourceMonitor(this))
            insertHandler(StatisticsHandler())
            setAttribute("is-default-server", true)
        }

        // UriCompliance.RFC3986 makes Jetty accept ambiguous values in path, so Javalin can handle them
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
    private val ServerConnector.baseUrl get() = "${this.protocol}://${this.host ?: "localhost"}:${this.localPort}${cfg.router.contextPath}"

}
