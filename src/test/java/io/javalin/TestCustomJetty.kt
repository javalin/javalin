/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.util.TestUtil
import org.eclipse.jetty.server.RequestLog
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.*
import org.eclipse.jetty.server.session.DefaultSessionCache
import org.eclipse.jetty.server.session.FileSessionDataStore
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class TestCustomJetty {

    @Test
    fun `embedded server can have custom jetty Handler`() {
        val statisticsHandler = StatisticsHandler()
        val server = Server().apply { handler = statisticsHandler }
        TestUtil.test(Javalin.create().server { server }) { app, http ->
            app.get("/") { ctx -> ctx.result("Hello World") }
            val requests = 5
            for (i in 0 until requests) {
                assertThat(http.getBody("/"), `is`("Hello World"))
                assertThat(http.get("/not_there").status, `is`(404))
            }
            assertThat(statisticsHandler.dispatched, `is`(requests * 2))
            assertThat(statisticsHandler.responses2xx, `is`(requests))
            assertThat(statisticsHandler.responses4xx, `is`(requests))
        }
    }

    @Test
    fun `embedded server can have custom jetty Handler chain`() {
        val logCount = AtomicLong(0)
        val requestLogHandler = RequestLogHandler().apply { requestLog = RequestLog { _, _ -> logCount.incrementAndGet() } }
        val handlerChain = StatisticsHandler().apply { handler = requestLogHandler }
        val server = Server().apply { handler = handlerChain }
        TestUtil.test(Javalin.create().server { server }) { app, http ->
            app.get("/") { ctx -> ctx.result("Hello World") }
            val requests = 10
            for (i in 0 until requests) {
                assertThat(http.getBody("/"), `is`("Hello World"))
                assertThat(http.get("/not_there").status, `is`(404))
            }
            assertThat(handlerChain.dispatched, `is`(requests * 2))
            assertThat(handlerChain.responses2xx, `is`(requests))
            assertThat(handlerChain.responses4xx, `is`(requests))
            assertThat(logCount.get(), `is`((requests * 2).toLong()))
        }
    }

    @Test
    fun `custom SessionHandler works`() {
        val server = Server()
        val fileSessionHandler = fileSessionHandler()
        val app = Javalin.create()
                .sessionHandler { fileSessionHandler }
                .server { server }
                .start()
        val httpHandler = (((server.handlers[0] as HandlerWrapper).handler as HandlerList).handlers.first() as ServletContextHandler)
        assertThat(httpHandler.sessionHandler, `is`(fileSessionHandler))
        app.stop()
    }

    @Test
    fun `custom ContextHandlerCollection works`() {
        val server = Server()
        val handler = ContextHandlerCollection().apply {
            setContextClass(ServletContextHandler::class.java)
            val ctx = addContext("/foo", ".") as ServletContextHandler
            ctx.addServlet(ServletHolder(object : HttpServlet() {
                override fun doGet(req: HttpServletRequest?, resp: HttpServletResponse?) {
                    resp?.writer?.write("yo dude")
                }
            }), "/foo")

        }
        server.handler = handler

        TestUtil.test(Javalin.create().server { server }) { app, http ->
            app.get("/bar") { ctx -> ctx.result("Hello") }
            assertThat(http.getBody("/foo/foo"), `is`("yo dude"))
            assertThat(http.get("/foo/baz").status, `is`(404))
            assertThat(http.getBody("/bar"), `is`("Hello"))

        }
    }

    private fun fileSessionHandler() = SessionHandler().apply {
        httpOnly = true
        sessionCache = DefaultSessionCache(this).apply {
            sessionDataStore = FileSessionDataStore().apply {
                val baseDir = File(System.getProperty("java.io.tmpdir"))
                this.storeDir = File(baseDir, "javalin-session-store").apply { mkdir() }
            }
        }
    }

}
