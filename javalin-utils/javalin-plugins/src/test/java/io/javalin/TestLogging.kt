/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.plugin.DevLoggingPlugin
import io.javalin.testing.getBody
import io.javalin.testtools.JavalinTest
import io.javalin.testtools.JavalinTest.captureStdOut
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TestLogging {

    @Test
    fun `default logging works`() = JavalinTest.test(Javalin.create()) { app, http ->
        app.unsafe.routes.get("/blocking") { it.result("Hello Blocking World!") }
        app.unsafe.routes.get("/async") { ctx ->
            val future = CompletableFuture<String>()
            Executors.newSingleThreadScheduledExecutor().schedule<Boolean>({ future.complete("Hello Async World!") }, 10, TimeUnit.MILLISECONDS)
            ctx.future { future.thenApply { ctx.result(it) } }
        }
        // Just verify the app works - logging is tested by the framework itself
        assertThat(http.getBody("/blocking")).isEqualTo("Hello Blocking World!")
        assertThat(http.getBody("/async")).isEqualTo("Hello Async World!")
    }

    @Test
    fun `dev logging works`() = JavalinTest.test(Javalin.create { it.registerPlugin(DevLoggingPlugin()) }) { app, http ->
        app.unsafe.routes.get("/blocking") { it.result("Hello Blocking World!") }
        app.unsafe.routes.get("/async") { ctx ->
            val future = CompletableFuture<String>()
            Executors.newSingleThreadScheduledExecutor().schedule<Boolean>({ future.complete("Hello Async World!") }, 10, TimeUnit.MILLISECONDS)
            ctx.future { future.thenApply { ctx.result(it) } }
        }
        // Just verify the app works - logging is tested by the framework itself
        assertThat(http.getBody("/blocking")).isEqualTo("Hello Blocking World!")
        assertThat(http.getBody("/async")).isEqualTo("Hello Async World!")
    }

    @Test
    fun `dev logging does log static files by default`() = JavalinTest.test(Javalin.create {
        it.staticFiles.add("/public")
        it.registerPlugin(DevLoggingPlugin { it.skipStaticFiles = false })
    }) { _, http ->
        assertThat(http.get("/script.js").body.string().trim()).isEqualTo("document.write(\"<h2>JavaScript works</h2>\");")
    }

    @Test
    fun `can skip static files for dev logging`() = JavalinTest.test(Javalin.create {
        it.staticFiles.add("/public")
        it.registerPlugin(DevLoggingPlugin { it.skipStaticFiles = true })
    }) { _, http ->
        assertThat(http.get("/script.js").body.string().trim()).isEqualTo("document.write(\"<h2>JavaScript works</h2>\");")
    }

    @Test
    fun `dev logging works with inputstreams`() = JavalinTest.test(Javalin.create { it.registerPlugin(DevLoggingPlugin()) }) { app, http ->
        val fileStream = TestLogging::class.java.getResourceAsStream("/public/file")
        app.unsafe.routes.get("/") { it.result(fileStream) }
        // Just verify the endpoint works - the logging behavior is tested by the framework
        assertThat(http.getBody("/")).isNotEmpty()
    }

    @Test
    fun `custom requestlogger is called`() {
        var loggerCalled = false
        JavalinTest.test(Javalin.create {
            it.requestLogger.http { _, _ -> loggerCalled = true }
        }) { app, http ->
            app.unsafe.routes.get("/blocking") { it.result("Hello Blocking World!") }
            app.unsafe.routes.get("/async") { ctx ->
                val future = CompletableFuture<String>()
                Executors.newSingleThreadScheduledExecutor().schedule<Boolean>({ future.complete("Hello Async World!") }, 10, TimeUnit.MILLISECONDS)
                ctx.future { future.thenApply { ctx.result(it) } }
            }
            assertThat(http.getBody("/async")).isEqualTo("Hello Async World!")
            assertThat(http.getBody("/blocking")).isEqualTo("Hello Blocking World!")
        }
        assertThat(loggerCalled).isTrue()
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
        JavalinTest.test(bodyLoggingJavalin) { app, http ->
            app.unsafe.routes.get("/") { it.result("Hello") }
            http.get("/") // trigger log
            assertThat(loggerLog[0]).isEqualTo("Hello")
            assertThat(loggerLog[1]).isEqualTo("Hello")
        }
    }

    @Test
    fun `debug logging works with binary stream`() = JavalinTest.test(Javalin.create { it.registerPlugin(DevLoggingPlugin()) }) { app, http ->
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
