/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.core.LoomUtil
import io.javalin.http.HttpCode
import io.javalin.testing.TestServlet
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.ForwardedRequestCustomizer
import org.eclipse.jetty.server.Handler
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.RequestLog
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.handler.ContextHandlerCollection
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.server.handler.RequestLogHandler
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.junit.Test
import java.io.File
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import javax.servlet.DispatcherType
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.FilterConfig
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class TestCustomJetty {

    @Test
    fun `setting port works`() {
        val port = (2000..9999).random()
        val app = Javalin.create().start(port).get("/") { it.result("PORT WORKS") }
        assertThat(Unirest.get("http://localhost:$port/").asString().body).isEqualTo("PORT WORKS")
        app.stop()
    }

    @Test
    fun `setting host works`() {
        val port = (2000..9999).random()
        val app = Javalin.create().start("127.0.0.1", port).get("/") { it.result("HOST WORKS") }
        assertThat(Unirest.get("http://127.0.0.1:$port/").asString().body).isEqualTo("HOST WORKS")
        app.stop()
    }

    @Test
    fun `embedded server can have custom jetty Handler`() {
        val statisticsHandler = StatisticsHandler()
        val newServer = Server().apply { handler = statisticsHandler }
        val app = Javalin.create { it.server { newServer } }.get("/") { it.result("Hello World") }.start(0)
        val requests = 5
        for (i in 0 until requests) {
            assertThat(Unirest.get("http://localhost:" + app.port() + "/").asString().body).isEqualTo("Hello World")
            assertThat(Unirest.get("http://localhost:" + app.port() + "/not-there").asString().status).isEqualTo(404)
        }
        app.stop()
        assertThat(statisticsHandler.dispatched).isEqualTo(requests * 2)
        assertThat(statisticsHandler.responses2xx).isEqualTo(requests)
        assertThat(statisticsHandler.responses4xx).isEqualTo(requests)
    }

    @Test
    fun `embedded server can have custom jetty Handler chain`() {
        val logCount = AtomicLong(0)
        val requestLogHandler = RequestLogHandler().apply { requestLog = RequestLog { _, _ -> logCount.incrementAndGet() } }
        val handlerChain = StatisticsHandler().apply { handler = requestLogHandler }
        val newServer = Server().apply { handler = handlerChain }
        val app = Javalin.create { it.server { newServer } }.get("/") { it.result("Hello World") }.start(0)
        val requests = 10
        for (i in 0 until requests) {
            assertThat(Unirest.get("http://localhost:" + app.port() + "/").asString().body).isEqualTo("Hello World")
            assertThat(Unirest.get("http://localhost:" + app.port() + "/not-there").asString().status).isEqualTo(404)
        }
        app.stop()
        assertThat(handlerChain.dispatched).`as`("dispatched").isEqualTo(requests * 2)
        assertThat(handlerChain.responses2xx).`as`("responses 2xx").isEqualTo(requests)
        assertThat(handlerChain.responses4xx).`as`("responses 4xx").isEqualTo(requests)
        assertThat(logCount.get()).`as`("logCount").isEqualTo((requests * 2).toLong())
    }

    @Test
    fun `embedded server can have a wrapped handler collection`() {
        val handlerCollection = HandlerCollection()
        val handlerChain = StatisticsHandler().apply { handler = handlerCollection }
        val newServer = Server().apply { handler = handlerChain }
        val app = Javalin.create { it.server { newServer } }.get("/") { it.result("Hello World") }.start(0)
        val requests = 10
        for (i in 0 until requests) {
            assertThat(Unirest.get("http://localhost:" + app.port() + "/").asString().body).isEqualTo("Hello World")
            assertThat(Unirest.get("http://localhost:" + app.port() + "/not-there").asString().status).isEqualTo(404)
        }
        app.stop()
        assertThat(handlerChain.dispatched).isEqualTo(requests * 2)
        assertThat(handlerChain.responses2xx).isEqualTo(requests)
        assertThat(handlerChain.responses4xx).isEqualTo(requests)
    }

    @Test
    fun `custom SessionHandler works`() {
        val newServer = Server()
        val fileSessionHandler = SessionHandler().apply {
            httpOnly = true
            sessionCache = DefaultSessionCache(this).apply {
                sessionDataStore = FileSessionDataStore().apply {
                    val baseDir = File(System.getProperty("java.io.tmpdir"))
                    this.storeDir = File(baseDir, "javalin-session-store-for-test").apply { mkdir() }
                }
            }
        }
        val javalin = Javalin.create {
            it.sessionHandler { fileSessionHandler }
            it.server { newServer }
        }.start(0)
        val httpHandler = (newServer.handlers[0] as ServletContextHandler)
        assertThat(httpHandler.sessionHandler).isEqualTo(fileSessionHandler)
        javalin.stop()

        val baseDir = File(System.getProperty("java.io.tmpdir"))
        File(baseDir, "javalin-session-store-for-test").deleteRecursively()
    }

    @Test
    fun `custom ContextHandlerCollection works`() {
        val newServer = Server()
        val handler = ContextHandlerCollection().apply {
            setContextClass(ServletContextHandler::class.java)
            val ctx = addContext("/foo", ".") as ServletContextHandler
            ctx.addServlet(ServletHolder(object : HttpServlet() {
                override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
                    resp?.writer?.write("yo dude")
                }
            }), "/foo")

        }
        newServer.handler = handler

        val javalin = Javalin.create { it.server { newServer } }
        TestUtil.test(javalin) { app, http ->
            app.get("/bar") { ctx -> ctx.result("Hello") }
            assertThat(http.getBody("/foo/foo")).isEqualTo("yo dude")
            assertThat(http.get("/foo/baz").status).isEqualTo(404)
            assertThat(http.getBody("/bar")).isEqualTo("Hello")
        }
    }

    @Test
    fun `custom Servlet works`() {
        val newServer = Server().apply {
            handler = ContextHandlerCollection().apply {
                handlers = arrayOf<Handler>(ServletContextHandler().apply {
                    contextPath = "/other-servlet"
                    addServlet(TestServlet::class.java, "/")
                })
            }
        }
        val javalin = Javalin.create {
            it.server { newServer }
            it.contextPath = "/api"
        }
        TestUtil.test(javalin) { app, http ->
            app.get("/") { ctx -> ctx.result("Hello Javalin World!") }
            assertThat(http.getBody("/api")).contains("Hello Javalin World!")
            assertThat(http.getBody("/other-servlet")).contains("Hello Servlet World!")
        }
    }

    @Test
    fun `default server uses loom if available`() {
        if (!LoomUtil.loomAvailable) return;
        val log = TestUtil.captureStdOut {
            Javalin.create().start(0).stop()
        }
        assertThat(log).contains("Loom is available, using Virtual ThreadPool... Neat!")
    }

    @Test
    fun `custom connector works`() {
        val port = (2000..9999).random()
        val app = Javalin.create { config ->
            config.server {
                Server().apply {
                    val httpConfiguration = HttpConfiguration()
                    httpConfiguration.addCustomizer(ForwardedRequestCustomizer())
                    val connector = ServerConnector(this, HttpConnectionFactory(httpConfiguration))
                    connector.port = port
                    this.addConnector(connector)
                }
            }
        }
        TestUtil.test(app) { server, _ ->
            server.get("/") { it.result("PORT WORKS") }
            assertThat(Unirest.get("http://localhost:$port/").asString().body).isEqualTo("PORT WORKS")
        }
    }

    @Test
    fun `can add filter to stop request before javalin`() {
        val filterJavalin = Javalin.create {
            it.configureServletContextHandler { handler ->
                handler.addFilter(FilterHolder(object : Filter {
                    override fun init(config: FilterConfig?) {}
                    override fun destroy() {}
                    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
                        (response as HttpServletResponse).status = HttpCode.IM_A_TEAPOT.status
                    }
                }), "/*", EnumSet.allOf(DispatcherType::class.java))
            }
        }
        TestUtil.test(filterJavalin) { app, http ->
            assertThat(http.get("/test").status).isEqualTo(HttpCode.IM_A_TEAPOT.status)
            assertThat(http.get("/test").body).isNotEqualTo("Test")
        }
    }

}

