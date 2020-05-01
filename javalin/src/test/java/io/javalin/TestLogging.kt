/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestLoggingUtil.captureStdOut
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TestLogging {

    @Test
    fun `default logging works`() {
        val log = captureStdOut { runTest(Javalin.create()) }
        println(log) // hard to test for absence ...
    }

    @Test
    fun `dev logging works`() {
        val log = captureStdOut { runTest(Javalin.create { it.enableDevLogging() }) }
        assertThat(log).contains("JAVALIN REQUEST DEBUG LOG")
        assertThat(log).contains("Hello Blocking World!")
        assertThat(log).contains("Hello Async World!")
    }

    @Test
    fun `custom requestlogger is called`() {
        var loggerCalled = false
        runTest(Javalin.create {
            it.requestLogger { _, _ -> loggerCalled = true }
        })
        assertThat(loggerCalled).isTrue()
    }

    private fun runTest(app: Javalin) {
        app.get("/blocking") { ctx -> ctx.result("Hello Blocking World!") }
        app.get("/async") { ctx ->
            val future = CompletableFuture<String>()
            Executors.newSingleThreadScheduledExecutor().schedule<Boolean>({ future.complete("Hello Async World!") }, 10, TimeUnit.MILLISECONDS)
            ctx.result(future)
        }
        app.start(0)
        assertThat(HttpUtil(app.port()).getBody("/async")).isEqualTo("Hello Async World!")
        assertThat(HttpUtil(app.port()).getBody("/blocking")).isEqualTo("Hello Blocking World!")
        app.stop()
    }

    @Test
    fun `resultString is available in request logger and can be read multiple times`() {
        val loggerLog = mutableListOf<String?>()
        val bodyLoggingJavalin = Javalin.create {
            it.requestLogger { ctx, ms ->
                loggerLog.add(ctx.resultString())
                loggerLog.add(ctx.resultString())
            }
        }
        TestUtil.test(bodyLoggingJavalin) { app, http ->
            app.get("/") { it.result("Hello") }
            http.get("/") // trigger log
            assertThat(loggerLog[0]).isEqualTo("Hello")
            assertThat(loggerLog[1]).isEqualTo("Hello")
        }
    }

    @Test
    fun `debug logging works with binary stream`() {
        val app = Javalin.create { it.enableDevLogging() }.start(0)
        app.get("/") {
            val imagePath = this::class.java.classLoader.getResource("upload-test/image.png")
            val stream = File(imagePath.toURI()).inputStream()
            it.result(stream)
        }
        val log = captureStdOut { Unirest.get("http://localhost:" + app.port() + "/").asString() }
        assertThat(log).contains("Body is binary (not logged)")
    }
}
