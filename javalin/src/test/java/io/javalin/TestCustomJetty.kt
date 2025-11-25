/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.http.Header
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.testing.TestServlet
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import io.javalin.util.LoomUtil
import jakarta.servlet.DispatcherType
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.ee10.servlet.FilterHolder
import org.eclipse.jetty.ee10.servlet.ServletContextHandler
import org.eclipse.jetty.ee10.servlet.ServletHolder
import org.eclipse.jetty.ee10.servlet.SessionHandler
import org.eclipse.jetty.server.ForwardedRequestCustomizer
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.RequestLog
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.session.DefaultSessionCache
import org.eclipse.jetty.session.FileSessionDataStore
import org.eclipse.jetty.util.resource.ResourceFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicLong

class TestCustomJetty {

    @TempDir
    lateinit var workingDirectory: File

    @Test
    fun `setting port works`() = TestUtil.runLogLess {
        val port = (2000..9999).random()
        val app = Javalin.create { it.routes.get("/") { it.result("PORT WORKS") } }.start(port)
        assertThat(Unirest.get("http://localhost:$port/").asString().body).isEqualTo("PORT WORKS")
        app.stop()
    }

    @Test
    fun `setting host works`() = TestUtil.runLogLess {
        val port = (2000..9999).random()
        val app = Javalin.create { it.routes.get("/") { it.result("HOST WORKS") } }.start("127.0.0.1", port)
        assertThat(Unirest.get("http://127.0.0.1:$port/").asString().body).isEqualTo("HOST WORKS")
        app.stop()
    }

    @Test
    fun `embedded server can have custom jetty Handler`() = TestUtil.runLogLess {
        val statisticsHandler = StatisticsHandler()
        val newServer = Server().apply { handler = statisticsHandler }
        val app = Javalin.create {
            it.jettyInternal.server = newServer
            it.routes.get("/") { it.result("Hello World") }
        }.start(0)
        val requests = 5
        for (i in 0 until requests) {
            assertThat(Unirest.get("http://localhost:" + app.port() + "/").asString().body).isEqualTo("Hello World")
            assertThat(Unirest.get("http://localhost:" + app.port() + "/not-there").asString().httpCode()).isEqualTo(NOT_FOUND)
        }
        app.stop()
        assertThat(statisticsHandler.handleTotal).isEqualTo(requests * 2)
        assertThat(statisticsHandler.responses2xx).isEqualTo(requests)
        assertThat(statisticsHandler.responses4xx).isEqualTo(requests)
    }

    @Test
    fun `embedded server can have custom jetty Handler chain`() = TestUtil.runLogLess {
        val logCount = AtomicLong(0)
        // Updated for Jetty 12 - RequestLogHandler integrated into server
        val handlerChain = StatisticsHandler()
        val newServer = Server().apply {
            handler = handlerChain
            requestLog = RequestLog { _, _ -> logCount.incrementAndGet() }
        }
        val app = Javalin.create {
            it.jettyInternal.server = newServer
            it.routes.get("/") { it.result("Hello World") }
        }.start(0)
        val requests = 10
        for (i in 0 until requests) {
            assertThat(Unirest.get("http://localhost:" + app.port() + "/").asString().body).isEqualTo("Hello World")
            assertThat(Unirest.get("http://localhost:" + app.port() + "/not-there").asString().httpCode()).isEqualTo(NOT_FOUND)
        }
        app.stop()
        assertThat(handlerChain.handleTotal).`as`("dispatched").isEqualTo(requests * 2)
        assertThat(handlerChain.responses2xx).`as`("responses 2xx").isEqualTo(requests)
        assertThat(handlerChain.responses4xx).`as`("responses 4xx").isEqualTo(requests)
        assertThat(logCount.get()).`as`("logCount").isEqualTo((requests * 2).toLong())
    }

    @Test
    fun `embedded server can have a wrapped handler collection`() = TestUtil.runLogLess {
        val handlerCollection = Handler.Sequence()
        val handlerChain = StatisticsHandler().apply { handler = handlerCollection }
        val newServer = Server().apply { handler = handlerChain }
        val app = Javalin.create {
            it.jettyInternal.server = newServer
            it.routes.get("/") { it.result("Hello World") }
        }.start(0)
        val requests = 10
        for (i in 0 until requests) {
            assertThat(Unirest.get("http://localhost:" + app.port() + "/").asString().body).isEqualTo("Hello World")
            assertThat(Unirest.get("http://localhost:" + app.port() + "/not-there").asString().httpCode()).isEqualTo(NOT_FOUND)
        }
        app.stop()
        assertThat(handlerChain.handleTotal).isEqualTo(requests * 2)
        assertThat(handlerChain.responses2xx).isEqualTo(requests)
        assertThat(handlerChain.responses4xx).isEqualTo(requests)
    }

    @Test
    fun `custom SessionHandler works`() = TestUtil.runLogLess {
        val newServer = Server()
        val fileSessionHandler = SessionHandler().apply {
            isHttpOnly = true
            sessionCache = DefaultSessionCache(this).apply {
                sessionDataStore = FileSessionDataStore().apply {
                    this.storeDir = workingDirectory
                }
            }
        }
        val javalin = Javalin.create {
            it.jetty.modifyServletContextHandler { it.sessionHandler = fileSessionHandler }
            it.jettyInternal.server = newServer
        }.start(0)
        val httpHandler = (newServer.handlers[0] as ServletContextHandler)
        assertThat(httpHandler.sessionHandler).isEqualTo(fileSessionHandler)
        javalin.stop()
    }

    @Test
    fun `custom ContextHandlerCollection works`() {
        val newServer = Server()
        val handler = ContextHandlerCollection().apply {
            val ctx = ServletContextHandler().apply {
                contextPath = "/foo"
                baseResource = ResourceFactory.root().newResource(".")
            }

            ctx.addServlet(ServletHolder(object : HttpServlet() {
                override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
                    resp?.writer?.write("yo dude")
                }
            }), "/foo")

            addHandler(ctx)
        }
        newServer.handler = handler

        val javalin = Javalin.create {
            it.jettyInternal.server = newServer
            it.routes.get("/bar") { it.result("Hello") }
        }
        TestUtil.test(javalin) { app, http ->
            assertThat(http.getBody("/foo/foo")).isEqualTo("yo dude")
            assertThat(http.get("/foo/baz").httpCode()).isEqualTo(NOT_FOUND)
            assertThat(http.getBody("/bar")).isEqualTo("Hello")
        }
    }

    @Test
    fun `custom Servlet works`() {
        val newServer = Server().apply {
            handler = ContextHandlerCollection().apply {
                handlers = listOf<Handler>(ServletContextHandler().apply {
                    contextPath = "/other-servlet"
                    addServlet(TestServlet::class.java, "/")
                })
            }
        }
        val javalin = Javalin.create {
            it.jettyInternal.server = newServer
            it.router.contextPath = "/api"
            it.routes.get("/") { it.result("Hello Javalin World!") }
        }
        TestUtil.test(javalin) { app, http ->

            assertThat(http.getBody("/api")).contains("Hello Javalin World!")
            assertThat(http.getBody("/other-servlet")).contains("Hello Servlet World!")
        }
    }

    @Test
    fun `default server uses loom (virtual threads) if enabled`() {
        if (!LoomUtil.loomAvailable) return
        val isVirtual = Thread::class.java.getMethod("isVirtual")
        val defaultApp = Javalin.create {
            it.useVirtualThreads = true
            it.routes.get("/") {
                val thread = Thread.currentThread()
                it.result("isVirtual:${isVirtual.invoke(thread)}|name:${thread.name}")
            }
        }
        TestUtil.test(defaultApp) { app, http ->
            val responseBody = http.get("/").body
            assertThat(responseBody).contains("isVirtual:true")
            assertThat(responseBody).contains("JettyServerThreadPool-Virtual")
        }
        assertThat(LoomUtil.isLoomThreadPool(defaultApp.jettyServer().server().threadPool)).isTrue
    }

    @Test
    fun `custom connector works`() {
        val port = (2000..9999).random()
        val app = Javalin.create { config ->
            config.jetty.addConnector { server, _ ->
                val httpConfiguration = HttpConfiguration()
                httpConfiguration.addCustomizer(ForwardedRequestCustomizer())
                val connector = ServerConnector(server, HttpConnectionFactory(httpConfiguration))
                connector.port = port
                connector
            }
            config.routes.get("/") { it.result("PORT WORKS") }
        }
        TestUtil.test(app) { server, _ ->
            assertThat(Unirest.get("http://localhost:$port/").asString().body).isEqualTo("PORT WORKS")
        }
    }

    @Test
    fun `can add filter to stop request before javalin`() {
        val filterJavalin = Javalin.create {
            it.jetty.modifyServletContextHandler { handler ->
                handler.addFilter(FilterHolder { req, res, chain ->
                    if ((req as HttpServletRequest).requestURI != "/allowed") {
                        res.writer.write("Not allowed")
                    } else {
                        chain.doFilter(req, res)
                    }
                }, "/*", EnumSet.allOf(DispatcherType::class.java))
            }
            it.routes.get("/allowed") { it.result("Allowed!") }
        }
        TestUtil.test(filterJavalin) { _, http ->
            assertThat(http.get("/allowed").body).isEqualTo("Allowed!")
            assertThat(http.get("/anything-else").body).isEqualTo("Not allowed")
        }
    }

    @Test
    fun `can add filter to do url rewrites based on content type`() {
        val filterJavalin = Javalin.create {
            it.jetty.modifyServletContextHandler { handler ->
                handler.addFilter(FilterHolder { req, res, chain ->
                    val contentType = req.getContentType()
                    val versionRegex = "vnd\\.blah\\.com\\+(v\\d)\\+json".toRegex()
                    val matchResult = versionRegex.find(contentType)
                    if (matchResult != null && req.getAttribute("forwarded") == null) {
                        val version = matchResult.groupValues[1] // extract version number
                        val newUri = "/$version${(req as HttpServletRequest).requestURI}"
                        req.setAttribute("forwarded", true)
                        req.getRequestDispatcher(newUri).forward(req, res)
                    } else {
                        chain.doFilter(req, res)
                    }
                }, "/*", EnumSet.allOf(DispatcherType::class.java))
            }
            it.routes.apiBuilder {
                get("/v1/test") { it.result("Version 1!") }
                get("/v2/test") { it.result("Version 2!") }
            }
        }
        TestUtil.test(filterJavalin) { _, http ->
            assertThat(http.get("/test", mapOf(Header.CONTENT_TYPE to "vnd.blah.com+v1+json")).body).isEqualTo("Version 1!")
            assertThat(http.get("/test", mapOf(Header.CONTENT_TYPE to "vnd.blah.com+v2+json")).body).isEqualTo("Version 2!")
        }
    }

}
