/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.plugin.bundled.DevLoggingPlugin
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import io.javalin.testing.TestUtil.captureStdOut
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.event.Level
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TestLogging {

    @Test
    fun `default logging works`() {
        val log = captureStdOut { runTest(Javalin.create()) }
        assertThat(log).contains("[main] INFO io.javalin.Javalin - Starting Javalin ...")
    }

    @Test
    fun `hideJettyLifecycleLogsBelowLevel hides Jetty startup and shutdown logs`() {
        val logWithJetty = captureStdOut { runTest(Javalin.create()) }
        val logWithoutJetty = captureStdOut { runTest(Javalin.create { it.startup.hideJettyLifecycleLogsBelowLevel = Level.WARN }) }
        // Default logging includes Jetty startup/shutodwn logs
        assertThat(logWithJetty).contains("org.eclipse.jetty.server.Server - jetty-12")
        assertThat(logWithJetty).contains("org.eclipse.jetty.session.DefaultSessionIdManager - Session workerName")
        assertThat(logWithJetty).contains("org.eclipse.jetty.server.Server - Started oejs.Server")
        assertThat(logWithJetty).contains("org.eclipse.jetty.server.Server - Stopped oejs.Server")
        assertThat(logWithJetty).contains("org.eclipse.jetty.server.AbstractConnector - Stopped oejs.ServerConnector")
        // With hideJettyLifecycleLogsBelowLevel set to WARN, all Jetty INFO logs should be hidden
        assertThat(logWithoutJetty).doesNotContain("org.eclipse.jetty")
        // But Javalin's own logs should still be present
        assertThat(logWithoutJetty).contains("Javalin started")
        assertThat(logWithoutJetty).contains("Javalin has stopped")
    }

    @Test
    fun `dev logging works`() {
        val log = captureStdOut { runTest(Javalin.create { it.registerPlugin(DevLoggingPlugin()) }) }
        assertThat(log).contains("JAVALIN REQUEST DEBUG LOG")
        assertThat(log).contains("Hello Blocking World!")
        assertThat(log).contains("Hello Async World!")
        assertThat(log).contains("JAVALIN HANDLER REGISTRATION DEBUG LOG: GET[/blocking]")
        assertThat(log).contains("JAVALIN HANDLER REGISTRATION DEBUG LOG: GET[/async]")
    }

    @Test
    fun `dev logging does log static files by default`() {
        assertThat(testStaticFiles(skipStaticFiles = false)).contains("script.js")
    }

    @Test
    fun `can skip static files for dev logging`() {
        assertThat(testStaticFiles(skipStaticFiles = true)).doesNotContain("script.js")
    }

    private fun testStaticFiles(skipStaticFiles: Boolean): String {
        return captureStdOut {
            TestUtil.test(Javalin.create {
                it.staticFiles.add("/public")
                it.registerPlugin(DevLoggingPlugin { it.skipStaticFiles = skipStaticFiles })
            }) { _, http ->
                assertThat(http.get("/script.js").body).isEqualTo("document.write(\"<h2>JavaScript works</h2>\");")
            }
        }
    }

    @Test
    fun `dev logging works with inputstreams`() = TestUtil.test(Javalin.create { it.registerPlugin(DevLoggingPlugin()) }) { app, http ->
        val fileStream = TestLogging::class.java.getResourceAsStream("/public/file")
        app.unsafe.routes.get("/") { it.result(fileStream) }
        val log = captureStdOut { http.getBody("/") }
        assertThat(log).doesNotContain("Stream closed")
        assertThat(log).contains("Body is an InputStream which can't be reset, so it can't be logged")
    }

    @Test
    fun `custom requestlogger is called`() {
        var loggerCalled = false
        TestUtil.runAndCaptureLogs {
            runTest(Javalin.create {
                it.requestLogger.http { _, _ -> loggerCalled = true }
            })
        }
        assertThat(loggerCalled).isTrue()
    }

    private fun runTest(app: Javalin) {
        app.unsafe.routes.get("/blocking") { it.result("Hello Blocking World!") }
        app.unsafe.routes.get("/async") { ctx ->
            val future = CompletableFuture<String>()
            Executors.newSingleThreadScheduledExecutor().schedule<Boolean>({ future.complete("Hello Async World!") }, 10, TimeUnit.MILLISECONDS)
            ctx.future { future.thenApply { ctx.result(it) } }
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
            it.requestLogger.http { ctx, _ ->
                loggerLog.add(ctx.result())
                loggerLog.add(ctx.result())
            }
        }
        TestUtil.test(bodyLoggingJavalin) { app, http ->
            app.unsafe.routes.get("/") { it.result("Hello") }
            http.get("/") // trigger log
            assertThat(loggerLog[0]).isEqualTo("Hello")
            assertThat(loggerLog[1]).isEqualTo("Hello")
        }
    }

    @Test
    fun `debug logging works with binary stream`() = TestUtil.test(Javalin.create { it.registerPlugin(DevLoggingPlugin()) }) { app, http ->
        app.unsafe.routes.get("/") {
            val imagePath = this::class.java.classLoader.getResource("upload-test/image.png")
            val stream = File(imagePath.toURI()).inputStream()
            it.result(stream)
        }
        val log = captureStdOut {
            http.getBody("/")
            http.getBody("/") // TODO: why must this be called twice on windows to avoid empty log output?
        }
        assertThat(log).contains("Body is binary (not logged)")
    }
}
