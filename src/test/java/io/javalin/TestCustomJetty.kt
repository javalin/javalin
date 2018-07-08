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
import org.eclipse.jetty.server.handler.RequestLogHandler
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.concurrent.atomic.AtomicLong

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
}
