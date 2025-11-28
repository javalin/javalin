package io.javalin

import io.javalin.http.Context
import io.javalin.http.HttpStatus
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.NotFoundResponse
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertThrows
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

@Timeout(value = 5, unit = SECONDS)
internal class TestFuture {

    private val scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    @Nested
    inner class FutureBasics {

        @Test
        fun `basic future works`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.future { getFuture("Result").thenApply { ctx.result(it) } }
            }
            assertThat(http.getBody("/")).isEqualTo("Result")
        }

        @Test
        fun `unresolved future is handled as error`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.future { getFuture(null) }
            }
            assertThat(http.getBody("/")).isEqualTo(INTERNAL_SERVER_ERROR.message)
        }

        @Test
        fun `multiple futures throw IllegalStateException`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.future { completedFuture("Success").thenApply { ctx.result(it) } }
                assertThrows<IllegalStateException> { ctx.future { completedFuture("Future") } }
                assertThrows<IllegalStateException> { ctx.async {} }
            }
            assertThat(http.getBody("/")).isEqualTo("Success")
        }

        @Test
        fun `async context is accessible in future supplier`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.future {
                    completedFuture(ctx.req().asyncContext.timeout).thenApply { ctx.result(it.toString()) }
                }
            }
            assertThat(http.getBody("/")).isEqualTo(app.unsafe.http.asyncTimeout.toString())
        }

        @Test
        fun `context can be used in thenAccept and exceptionally`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.future {
                    getFuture(ctx.queryParam("qp"))
                        .thenAccept { ctx.result(it) }
                        .exceptionally {
                            ctx.result("Error: $it")
                            null
                        }
                }
            }
            assertThat(http.get("/?qp=Hello").body).isEqualTo("Hello")
            assertThat(http.get("/").body).isEqualTo("Error: java.util.concurrent.CompletionException: java.util.concurrent.CancellationException")
        }

        @Test
        fun `exceptions can be thrown inside thenAccept`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.future { getFuture("A").thenAccept { throw NotFoundResponse() } }
            }
            assertThat(http.get("/").status).isEqualTo(HttpStatus.NOT_FOUND.code)
            assertThat(http.get("/").body).isEqualTo(HttpStatus.NOT_FOUND.message)
        }

        @Test
        fun `nested futures work in callbacks`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.future {
                    getFuture("A", delay = 100).thenAccept {
                        ctx.accumulatingResult(it)
                        ctx.future {
                            getFuture("B", delay = 0).thenAccept {
                                ctx.accumulatingResult(it)
                            }
                        }
                    }
                }
            }
            assertThat(http.getBody("/")).isEqualTo("AB")
        }

    }

    @Nested
    inner class FutureLifecycle {

        @Test
        fun `after-handlers run after future completes`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { it.future { getFuture("Not result") } }
            app.unsafe.routes.after { it.result("Overwritten by after-handler") }
            assertThat(http.getBody("/")).isEqualTo("Overwritten by after-handler")
        }

        @Test
        fun `error-handlers run after future completes`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { it.status(INTERNAL_SERVER_ERROR).future { getFuture("Not result") } }
            app.unsafe.routes.error(INTERNAL_SERVER_ERROR) { it.result("Overwritten by error-handler") }
            assertThat(http.getBody("/")).isEqualTo("Overwritten by error-handler")
        }

        @Test
        fun `future can be called in before, endpoint, and after handlers`() = TestUtil.test { app, http ->
            app.unsafe.routes.before("/") { ctx -> ctx.future { getFuture("before").thenApply { ctx.result(it) } } }
            app.unsafe.routes.get("/") { ctx -> ctx.future { getFuture("nothing") } }
            app.unsafe.routes.after("/") { ctx -> ctx.future { getFuture("${ctx.result()}, after").thenApply { ctx.result(it) } } }
            assertThat(http.get("/").body).isEqualTo("before, after")
        }

        @Test
        fun `future can be called in multiple before handlers`() = TestUtil.test { app, http ->
            app.unsafe.routes.before { it.future { getFuture("before 1").thenAccept { v -> it.result(v) } } }
            app.unsafe.routes.before { it.future { getFuture("${it.result()}, before 2").thenAccept { v -> it.result(v) } } }
            app.unsafe.routes.get("/") {}
            assertThat(http.get("/").body).isEqualTo("before 1, before 2")
        }

        @Test
        fun `future can be used in exception mapper`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { throw Exception("Oh no!") }
            app.unsafe.routes.exception(Exception::class.java) { _, ctx ->
                ctx.future { getFuture("Handled").thenApply { ctx.result(it) } }
            }
            assertThat(http.get("/").body).isEqualTo("Handled")
        }

        @Test
        fun `after-handlers run after future exception`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.future { getFailingFuture(UnsupportedOperationException()) }
            }.exception(UnsupportedOperationException::class.java) { _, ctx ->
                ctx.accumulatingResult("exception")
            }.after { ctx ->
                ctx.accumulatingResult("+after")
            }
            assertThat(http.get("/").body).isEqualTo("exception+after")
        }

    }

    @Nested
    inner class FutureExceptions {

        @Test
        fun `unresolved future is handled by exception mapper`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { it.future { getFuture(null) } }
            app.unsafe.routes.exception(CancellationException::class.java) { _, ctx -> ctx.result("Handled") }
            assertThat(http.getBody("/")).isEqualTo("Handled")
        }

        @Test
        fun `exception in future supplier is handled`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { it.future { throw IllegalStateException("Error") } }
            app.unsafe.routes.exception(IllegalStateException::class.java) { _, ctx -> ctx.result("Handled") }
            assertThat(http.getBody("/")).isEqualTo("Handled")
        }

        @Test
        fun `future failure is handled by exception mapper`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.future { getFailingFuture(UnsupportedOperationException()) }
            }
            app.unsafe.routes.exception(UnsupportedOperationException::class.java) { _, ctx -> ctx.result("Handled") }
            assertThat(http.getBody("/")).isEqualTo("Handled")
        }

        @Test
        fun `error is handled as unexpected throwable`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/sync-error") { throw OutOfMemoryError() }
            assertThat(http.getStatus("/sync-error")).isEqualTo(INTERNAL_SERVER_ERROR)

            app.unsafe.routes.get("/future-error") { it.future { getFailingFuture(OutOfMemoryError()) } }
            assertThat(http.getStatus("/future-error")).isEqualTo(INTERNAL_SERVER_ERROR)
        }

        @Test
        fun `exception during response writing is handled`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.future { getFutureFailingStream().thenApply { ctx.result(it) } }
            }
            assertThat(http.get("/").body).isEmpty()
            assertThat(http.get("/").httpCode()).isEqualTo(INTERNAL_SERVER_ERROR)
        }

    }

    @Nested
    inner class FutureTimeouts {

        private val impatientServer = Javalin.create { it.http.asyncTimeout = 5 }

        @Test
        fun `default timeout returns REQUEST_TIMEOUT message`() = TestUtil.test(impatientServer) { app, http ->
            app.unsafe.routes.get("/") { it.future { getFuture("Test", delay = 5000) } }
            assertThat(http.get("/").body).isEqualTo(HttpStatus.REQUEST_TIMEOUT.message)
        }

        @Test
        fun `timeout can be overridden with error handler`() = TestUtil.test(impatientServer) { app, http ->
            app.unsafe.routes.get("/") { it.future { getFuture("Test", delay = 5000) } }
            app.unsafe.routes.error(INTERNAL_SERVER_ERROR) { it.result("Custom timeout message") }
            assertThat(http.get("/").body).isEqualTo("Custom timeout message")
        }

        @Test
        fun `timed out future is canceled`() = TestUtil.test(impatientServer) { app, http ->
            val future = getFuture("Test", delay = 5000)
            app.unsafe.routes.get("/") { it.future { future } }
            assertThat(http.get("/").body).isEqualTo(HttpStatus.REQUEST_TIMEOUT.message)
            assertThat(future.isCancelled).isTrue()
        }

        @Test
        fun `latest future is canceled on timeout`() = TestUtil.test(impatientServer) { app, http ->
            app.unsafe.routes.before { it.future { completedFuture("Success") } }
            val future = getFuture("Test", delay = 5000)
            app.unsafe.routes.get("/") { it.future { future } }
            assertThat(http.get("/").body).isEqualTo(HttpStatus.REQUEST_TIMEOUT.message)
            assertThat(future.isCancelled).isTrue()
        }

        @Test
        fun `after-handlers run after default timeout`() = TestUtil.test(impatientServer) { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.future { getFuture("Test", delay = 5000) }
            }.after { ctx ->
                ctx.accumulatingResult("+after")
            }
            assertThat(http.get("/").body).contains("after")
        }

    }

    @Nested
    inner class AsyncBasics {

        @Test
        fun `async executes on different thread`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                val httpThreadName = Thread.currentThread().name
                ctx.async {
                    ctx.result((Thread.currentThread().name != httpThreadName).toString())
                }
            }
            assertThat(http.get("/").body).isEqualTo("true")
        }

        @Test
        fun `nested async tasks execute in order`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.async {
                    ctx.async {
                        ctx.accumulatingResult("3")
                    }
                    Thread.sleep(10)
                    ctx.accumulatingResult("2")
                }
                Thread.sleep(40)
                ctx.accumulatingResult("1")
            }
            assertThat(http.get("/").body).isEqualTo("123")
        }

        @Test
        fun `async exception is handled by exception mapper`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.async { throw UnsupportedOperationException() }
            }.exception(UnsupportedOperationException::class.java) { _, ctx ->
                ctx.result("Handled")
            }
            assertThat(http.get("/").body).isEqualTo("Handled")
        }

        @Test
        fun `async with custom timeout works`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.async({ config ->
                    config.timeout = 10L
                    config.onTimeout { it.result("Timeout") }
                }) {
                    Thread.sleep(500L)
                    ctx.result("Result")
                }
            }
            assertThat(http.get("/").body).isEqualTo("Timeout")
        }

        @Test
        fun `async with custom timeout that throws exception is handled`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.async({ config ->
                    config.timeout = 10L
                    config.onTimeout { throw UnsupportedOperationException() }
                }) {
                    Thread.sleep(500L)
                    ctx.result("Result")
                }
            }.exception(UnsupportedOperationException::class.java) { _, ctx ->
                ctx.result("Exception handled")
            }
            assertThat(http.get("/").body).isEqualTo("Exception handled")
        }

    }

    @Nested
    inner class AsyncLifecycle {

        @Test
        fun `after-handlers run after async completes`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.async { ctx.accumulatingResult("result") }
            }.after { ctx ->
                ctx.accumulatingResult("+after")
            }
            assertThat(http.get("/").body).isEqualTo("result+after")
        }

        @Test
        fun `after-handlers run after async exception`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.async { throw UnsupportedOperationException() }
            }.exception(UnsupportedOperationException::class.java) { _, ctx ->
                ctx.accumulatingResult("exception")
            }.after { ctx ->
                ctx.accumulatingResult("+after")
            }
            assertThat(http.get("/").body).isEqualTo("exception+after")
        }

        @Test
        fun `after-handlers run after async timeout`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.async({ config ->
                    config.timeout = 10L
                    config.onTimeout { it.accumulatingResult("timeout") }
                }) {
                    Thread.sleep(500L)
                    ctx.result("result")
                }
            }.after { ctx ->
                ctx.accumulatingResult("+after")
            }
            assertThat(http.get("/").body).isEqualTo("timeout+after")
        }

        @Test
        fun `after-handlers run after async timeout exception`() = TestUtil.test { app, http ->
            app.unsafe.routes.get("/") { ctx ->
                ctx.async({ config ->
                    config.timeout = 10L
                    config.onTimeout { throw UnsupportedOperationException() }
                }) {
                    Thread.sleep(500L)
                    ctx.result("result")
                }
            }.exception(UnsupportedOperationException::class.java) { _, ctx ->
                ctx.accumulatingResult("exception")
            }.after { ctx ->
                ctx.accumulatingResult("+after")
            }
            assertThat(http.get("/").body).isEqualTo("exception+after")
        }

        @Test
        fun `before-matched and after-matched work with async`() = TestUtil.test { app, http ->
            app.unsafe.routes.beforeMatched("/") { ctx ->
                ctx.accumulatingResult("before-matched,")
            }
            app.unsafe.routes.get("/") { ctx ->
                ctx.async { ctx.accumulatingResult("async,") }
            }
            app.unsafe.routes.afterMatched("/") { ctx ->
                ctx.accumulatingResult("after-matched")
            }
            assertThat(http.get("/").body).isEqualTo("before-matched,async,after-matched")
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

private fun Context.accumulatingResult(s: String) = this.result((result() ?: "") + s)
