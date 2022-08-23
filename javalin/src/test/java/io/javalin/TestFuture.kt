package io.javalin

import io.javalin.http.ContentType.Companion.JSON
import io.javalin.http.ContentType.Companion.PLAIN
import io.javalin.http.Header
import io.javalin.http.HttpStatus.ENHANCE_YOUR_CALM
import io.javalin.http.HttpStatus.IM_A_TEAPOT
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.NO_CONTENT
import io.javalin.http.HttpStatus.OK
import io.javalin.http.NotFoundResponse
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

@Timeout(value = 5, unit = SECONDS)
internal class TestFuture {

    private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    @Test
    fun `hello future world`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.future(getFuture("Result").thenAccept { v -> it.result(v) }) }
        assertThat(http.getBody("/test-future")).isEqualTo("Result")
    }

    @Test
    fun `unresolved future throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.future(getFuture(null)) }
        assertThat(http.getBody("/test-future")).isEqualTo(INTERNAL_SERVER_ERROR.message)
    }

    @Nested
    inner class Lifecycle {

        @Test
        fun `after-handlers run after future is resolved`() = TestUtil.test { app, http ->
            app.get("/test-future") { it.future(getFuture("Not result")) }
            app.after { it.result("Overwritten by after-handler") }
            assertThat(http.getBody("/test-future")).isEqualTo("Overwritten by after-handler")
        }

        @Test
        fun `error-handlers run after future is resolved`() = TestUtil.test { app, http ->
            app.get("/test-future") { it.status(INTERNAL_SERVER_ERROR).future(getFuture("Not result")) }
            app.error(INTERNAL_SERVER_ERROR) { it.result("Overwritten by error-handler") }
            assertThat(http.getBody("/test-future")).isEqualTo("Overwritten by error-handler")
        }

        @Test
        fun `calling future in (before - get - after) handlers works`() = TestUtil.test { app, http ->
            app.before("/future") { it.future(getFuture("before").thenAccept { v -> it.result(v) }) }
            app.get("/future") { it.future(getFuture("nothing")) }
            app.after("/future") { it.future(getFuture("${it.resultString()}, after").thenAccept { v -> it.result(v) }) }
            assertThat(http.get("/future").body).isEqualTo("before, after")
        }

        @Test
        fun `can use future in exception mapper`() = TestUtil.test { app, http ->
            app.get("/") { throw Exception("Oh no!") }
            app.exception(Exception::class.java) { _, ctx ->
                ctx.future(getFuture("Wee").thenAccept { ctx.result(it) })
            }
            assertThat(http.get("/").body).isEqualTo("Wee")
        }

    }

    @Nested
    inner class Exceptions {

        @Test
        fun `can call ctx inside thenAccept and exceptionally`() = TestUtil.test { app, http ->
            app.get("/") { ctx ->
                ctx.future(getFuture(ctx.queryParam("qp")) // could be null, which would cause a CancellationException
                    .thenAccept { ctx.result(it) }
                    .exceptionally {
                        ctx.result("Error: $it")
                        null
                    })
            }
            assertThat(http.get("/?qp=Hello").body).isEqualTo("Hello")
            assertThat(http.get("/").body).isEqualTo("Error: java.util.concurrent.CompletionException: java.util.concurrent.CancellationException")
        }

        @Test
        fun `unresolved futures are handled by exception-mapper`() = TestUtil.test { app, http ->
            app.get("/test-future") { it.future(getFuture(null)) }
            app.exception(CancellationException::class.java) { _, ctx -> ctx.result("Handled") }
            assertThat(http.getBody("/test-future")).isEqualTo("Handled")
        }

        @Test
        fun `futures failures are handled by exception-mapper`() = TestUtil.test { app, http ->
            app.get("/test-future") { ctx ->
                ctx.future(getFailingFuture(UnsupportedOperationException()))
            }
            app.exception(UnsupportedOperationException::class.java) { _, ctx -> ctx.result("Handled") }
            assertThat(http.getBody("/test-future")).isEqualTo("Handled")
        }

        @Test
        fun `error is handled as unexpected throwable`() = TestUtil.test { app, http ->
            app.get("/out-of-memory") { throw OutOfMemoryError() }
            assertThat(http.getStatus("/out-of-memory")).isEqualTo(INTERNAL_SERVER_ERROR)

            app.get("/out-of-memory-future") { it.future(getFailingFuture(OutOfMemoryError())) }
            assertThat(http.getStatus("/out-of-memory-future")).isEqualTo(INTERNAL_SERVER_ERROR)
        }

        @Test
        fun `exceptions that occur during response writing are handled`() = TestUtil.test { app, http ->
            app.get("/test-future") { it.future(getFutureFailingStream().thenAccept { v -> it.result(v) }) }
            assertThat(http.get("/test-future").body).isEmpty()
            assertThat(http.get("/test-future").httpCode()).isEqualTo(INTERNAL_SERVER_ERROR)
        }

        @Test
        fun `user's future should be cancelled in case of exception in handler`() = TestUtil.test { app, http ->
            val future = CompletableFuture<String>()
            app.get("/") {
                it.future(future)
                throw RuntimeException()
            }
            assertThat(http.get("/").body).isEqualTo(INTERNAL_SERVER_ERROR.message)
            assertThat(future.isCancelled).isTrue()
        }

    }

    @Nested
    inner class Timeouts {

        private val impatientServer = Javalin.create { it.http.asyncTimeout = 5 }

        @Test
        fun `default timeout error isn't jetty branded`() = TestUtil.test(impatientServer) { app, http ->
            app.get("/") { it.future(getFuture("Test", delay = 5000)) }
            assertThat(http.get("/").body).isEqualTo("Request timed out")
        }

        @Test
        fun `can override timeout with custom error message`() = TestUtil.test(impatientServer) { app, http ->
            app.get("/") { it.future(getFuture("Test", delay = 5000)) }
            app.error(INTERNAL_SERVER_ERROR) { it.result("My own simple error message") }
            assertThat(http.get("/").body).isEqualTo("My own simple error message")
        }

        @Test
        fun `timed out futures are canceled`() = TestUtil.test(impatientServer) { app, http ->
            val future = getFuture("Test", delay = 5000)
            app.get("/") { it.future(future) }
            assertThat(http.get("/").body).isEqualTo("Request timed out")
            assertThat(future.isCancelled).isTrue()
        }

        @Test
        fun `latest timed out future is canceled`() = TestUtil.test(impatientServer) { app, http ->
            app.before { it.future(CompletableFuture.completedFuture("Success")) }
            val future = getFuture("Test", delay = 5000)
            app.get("/") { it.future(future) }
            assertThat(http.get("/").body).isEqualTo("Request timed out")
            assertThat(future.isCancelled).isTrue()
        }

    }

    @Nested
    inner class Async {

        @Test
        fun `async requests works`() = TestUtil.test { app, http ->
            app.get("/") { ctx ->
                val httpThreadName = Thread.currentThread().name

                ctx.async {
                    ctx.result((Thread.currentThread().name != httpThreadName).toString())
                }
            }

            assertThat(http.get("/").body).isEqualTo("true")
        }

        @Test
        fun `async tasks should start execution in a proper order`() = TestUtil.test { app, http ->
            app.get("/") { ctx ->
                ctx.async {
                    ctx.async {
                        ctx.status(OK)
                    }
                    Thread.sleep(100)
                    ctx.status(ENHANCE_YOUR_CALM)
                }
                Thread.sleep(100)
                ctx.status(IM_A_TEAPOT)
            }

            assertThat(http.get("/").httpCode()).isEqualTo(OK)
        }

        @Test
        fun `exception in async works`() = TestUtil.test { app, http ->
            app
                .get("/") { ctx ->
                    ctx.async { throw UnsupportedOperationException() }
                }
                .exception(UnsupportedOperationException::class.java) { error, ctx ->
                    ctx.result("Unsupported")
                }

            assertThat(http.get("/").body).isEqualTo("Unsupported")
        }

        @Test
        fun `timeout should work`() = TestUtil.test { app, http ->
            app.get("/") { ctx ->
                ctx.async(
                    timeout = 10L,
                    onTimeout = { ctx.result("Timeout") },
                    task = {
                        Thread.sleep(500L)
                        ctx.result("Result")
                    }
                )
            }

            assertThat(http.get("/").body).isEqualTo("Timeout")
        }

    }

    private fun getFailingFuture(failure: Throwable): CompletableFuture<String> {
        return CompletableFuture.supplyAsync { throw failure }
    }

    private fun getFutureFailingStream(): CompletableFuture<InputStream> =
        getFuture(object : InputStream() {
            override fun read(): Int {
                throw IOException()
            }
        })

    private fun <T> getFuture(result: T?, delay: Long = 10): CompletableFuture<T> =
        CompletableFuture<T>().also {
            scheduledExecutorService.schedule({
                when {
                    result != null -> it.complete(result)
                    else -> it.cancel(false)
                }
            }, delay, TimeUnit.MILLISECONDS)
        }

}
