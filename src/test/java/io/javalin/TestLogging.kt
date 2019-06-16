/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.misc.HttpUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TestLogging {

    @Test
    fun `default logging (no logging) works`() = runTest(Javalin.create())

    @Test
    fun `debug logging works`() = runTest(Javalin.create { it.enableDevLogging() })

    @Test
    fun `custom logging works`() = runTest(Javalin.create {
        it.requestLogger { _, executionTimeMs ->
            println("That took $executionTimeMs milliseconds")
        }
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
    private val bodyLoggingJavalin = Javalin.create {
        it.requestLogger { ctx, ms ->
            loggerLog.add(ctx.resultString())
            loggerLog.add(ctx.resultString())
        }
    }

    @Test
    fun `resultString is available in logger and can be read twice`() = TestUtil.test(bodyLoggingJavalin) { app, http ->
        app.get("/") { it.result("Hello") }
        http.get("/") // trigger log
        assertThat(loggerLog[0]).isEqualTo("Hello")
        assertThat(loggerLog[1]).isEqualTo("Hello")
    }

    @Test
    fun `debug logging works with binary stream`() {
        val log = captureStdOut {
            TestUtil.test(Javalin.create {
                it.enableDevLogging()
            }) { app, http ->
                app.get("/") {
                    val imagePath = this::class.java.classLoader.getResource("upload-test/image.png")
                    val stream = File(imagePath.toURI()).inputStream()
                    it.result(stream)
                }
                http.get("/") // trigger log
            }
        }
        assertThat(log).contains("Body is binary (not logged)")
    }

}

fun captureStdOut(run: () -> Unit): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteArrayOutputStream)
    val oldOut = System.out
    val oldErr = System.err
    System.setOut(printStream)
    System.setErr(printStream)

    try {
        run()
    } finally {
        System.out.flush()
        System.setOut(oldOut)
        System.setErr(oldErr)
        println("Captured output:\n$byteArrayOutputStream")
    }

    return byteArrayOutputStream.toString()
}
