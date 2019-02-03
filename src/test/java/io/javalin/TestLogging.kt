/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.util.HttpUtil
import io.javalin.util.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TestLogging {

    @Test
    fun `default logging (no logging) works`() = runTest(Javalin.create())

    @Test
    fun `debug logging works`() = runTest(Javalin.create().enableDebugLogging())

    @Test
    fun `custom logging works`() = runTest(Javalin.create().requestLogger { _, executionTimeMs ->
        println("That took $executionTimeMs milliseconds")
    })

    private fun runTest(app: Javalin) {
        app.get("/blocking") { ctx -> ctx.result("Hello Blocking World!") }
        app.get("/async") { ctx ->
            val future = CompletableFuture<String>()
            Executors.newSingleThreadScheduledExecutor().schedule<Boolean>({ future.complete("Hello Async World!") }, 10, TimeUnit.MILLISECONDS)
            ctx.result(future)
        }
        app.start(0)
        val http = HttpUtil(app)
        assertThat(http.getBody("/async")).isEqualTo("Hello Async World!")
        assertThat(http.getBody("/blocking")).isEqualTo("Hello Blocking World!")
        app.stop()
    }

    private val loggerLog = mutableListOf<String?>()
    private val bodyLoggingJavalin = Javalin.create().requestLogger { ctx, ms ->
        loggerLog.add(ctx.resultString())
        loggerLog.add(ctx.resultString())
    }

    @Test
    fun `resultString is available in logger and can be read twice`() = TestUtil.test(bodyLoggingJavalin) { app, http ->
        app.get("/") { it.result("Hello") }
        http.get("/") // trigger log
        assertThat(loggerLog[0]).isEqualTo("Hello")
        assertThat(loggerLog[1]).isEqualTo("Hello")
    }

}
