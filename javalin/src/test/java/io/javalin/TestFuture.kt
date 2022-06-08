package io.javalin

import io.javalin.core.util.Header
import io.javalin.http.ContentType
import io.javalin.http.HttpCode.INTERNAL_SERVER_ERROR
import io.javalin.http.NotFoundResponse
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TestFuture {

    @Test
    fun `hello future world`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.future(getFuture("Result")) }
        assertThat(http.getBody("/test-future")).isEqualTo("Result")
    }

    @Test
    fun `after-handlers run after future is resolved`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.future(getFuture("Not result")) }
        app.after { it.result("Overwritten by after-handler") }
        assertThat(http.getBody("/test-future")).isEqualTo("Overwritten by after-handler")
    }

    @Test
    fun `error-handlers run after future is resolved`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.status(555).future(getFuture("Not result")) }
        app.error(555) { it.result("Overwritten by error-handler") }
        assertThat(http.getBody("/test-future")).isEqualTo("Overwritten by error-handler")
    }

    @Test
    fun `unresolved future throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.future(getFuture(null)) }
        assertThat(http.getBody("/test-future")).isEqualTo("Internal server error")
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
    fun `errors are handled as unexpected throwables`() = TestUtil.test { app, http ->
        app.get("/out-of-memory") { throw OutOfMemoryError() }
        assertThat(http.getStatus("/out-of-memory")).isEqualTo(INTERNAL_SERVER_ERROR)

        app.get("/out-of-memory-future") { it.future(getFailingFuture(OutOfMemoryError())) }
        assertThat(http.getStatus("/out-of-memory-future")).isEqualTo(INTERNAL_SERVER_ERROR)
    }

    @Test
    fun `future is overwritten if String result is set`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx ->
            ctx.future(getFuture("Result"))
            ctx.result("Overridden")
        }
        assertThat(http.getBody("/test-future")).isEqualTo("Overridden")
    }

    @Test
    fun `calling future twice cancels first future`() = TestUtil.test { app, http ->
        val firstFuture = getFuture("Result", delay = 5000)
        app.get("/test-future") { ctx ->
            ctx.future(firstFuture)
            ctx.future(CompletableFuture.completedFuture("Second future"))
        }
        assertThat(http.getBody("/test-future")).isEqualTo("Second future")
        assertThat(firstFuture.isCancelled).isTrue()
    }

    @Test
    fun `calling future in (before - get - after) handlers works`() = TestUtil.test { app, http ->
        app.before("/future") { it.future(getFuture("before")) }
        app.get("/future") { it.future(getFuture("nothing")) { /* do nothing */ } }
        app.after("/future") { it.future(getFuture("${it.resultString()}, after")) }
        assertThat(http.get("/future").body).isEqualTo("before, after")
    }

    @Test
    fun `exceptions that occur during response writing are handled`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.future(getFutureFailingStream()) }
        assertThat(http.get("/test-future").body).isEqualTo("")
        assertThat(http.get("/test-future").status).isEqualTo(500)
    }

    @Test
    fun `loonyrunes is happy with the api`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val hasContent = ctx.queryParam("has-content") != null
            ctx.future(getFuture("some-future-result")) { result ->
                if (hasContent) {
                    ctx.status(200)
                    ctx.json(result!!)
                } else {
                    ctx.status(204)
                }
            }
        }
        val contentResponse = http.get("/?has-content")
        assertThat(contentResponse.status).isEqualTo(200)
        assertThat(contentResponse.body).isEqualTo("""some-future-result""")
        assertThat(contentResponse.headers.getFirst(Header.CONTENT_TYPE)).isEqualTo(ContentType.JSON)
        val noContentResponse = http.get("/?no-content")
        assertThat(noContentResponse.status).isEqualTo(204)
        assertThat(noContentResponse.body).isEqualTo("")
        assertThat(noContentResponse.headers.getFirst(Header.CONTENT_TYPE)).isEqualTo(ContentType.PLAIN)
    }

    @Test
    fun `exceptions in future callback are mapped`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            ctx.future(getFuture("result")) { throw NotFoundResponse() }
        }
        assertThat(http.get("/").status).isEqualTo(404)
    }

    @Test
    fun `can use future in exception mapper`() = TestUtil.test { app, http ->
        app.get("/") { throw Exception("Oh no!") }
        app.exception(Exception::class.java) { _, ctx -> ctx.future(CompletableFuture.completedFuture("Wee")) }
        assertThat(http.get("/").body).isEqualTo("Wee")
    }

    private val impatientServer: Javalin by lazy { Javalin.create { it.asyncRequestTimeout = 5 } }

    @Test
    fun `default timeout error isn't jetty branded`() = TestUtil.test(impatientServer) { app, http ->
        app.get("/") { it.future(getFuture("Test", delay = 5000)) }
        assertThat(http.get("/").body).isEqualTo("Request timed out")
    }

    @Test
    fun `can override timeout with custom error message`() = TestUtil.test(impatientServer) { app, http ->
        app.get("/") { it.future(getFuture("Test", delay = 5000)) }
        app.error(500) { it.result("My own simple error message") }
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
    fun `user's future should be cancelled in case of exception in handler`() = TestUtil.test(impatientServer) { app, http ->
        val future = CompletableFuture<String>()
        app.get("/") {
            it.future(future)
            throw RuntimeException()
        }
        assertThat(http.get("/").body).isEqualTo("Internal server error")
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

    @Test
    fun `can set default callback via context resolvers`() {
        val ignoringServer = Javalin.create {
            it.contextResolvers { it.defaultFutureCallback = { ctx, _ -> ctx.result("Ignore result") } }
        }
        TestUtil.test(ignoringServer) { app, http ->
            app.get("/") { it.future(CompletableFuture.completedFuture("Success")) }
            assertThat(http.get("/").body).isEqualTo("Ignore result")
        }
    }

    @Test
    fun `should support legacy usage of asyncStart`() = TestUtil.test(impatientServer) { app, http ->
        app.get("/") { ctx ->
            ctx.req.startAsync()

            getFuture("response").thenAccept {
                ctx.res.outputStream.write(it.toByteArray())
                ctx.req.asyncContext.complete()
            }
        }

        assertThat(http.get("/").body).isEqualTo("response")
    }

    private fun getFuture(result: String?, delay: Long = 10): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        Executors.newSingleThreadScheduledExecutor().schedule({
            if (result != null) {
                future.complete(result)
            } else {
                future.cancel(false)
            }
        }, delay, TimeUnit.MILLISECONDS)
        return future
    }

    private fun getFailingFuture(failure: Throwable): CompletableFuture<String> {
        return CompletableFuture.supplyAsync { throw failure }
    }

    private fun getFutureFailingStream(): CompletableFuture<InputStream> {
        val future = CompletableFuture<InputStream>()
        Executors.newSingleThreadScheduledExecutor().schedule({
            val stream = object : InputStream() {
                override fun read(): Int {
                    throw IOException()
                }
            }
            future.complete(stream)
        }, 10, TimeUnit.MILLISECONDS)
        return future
    }

}
