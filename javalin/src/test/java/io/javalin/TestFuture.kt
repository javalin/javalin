package io.javalin

import io.javalin.core.util.Header
import io.javalin.http.ContentType
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
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
        app.after { ctx -> ctx.result("Overwritten by after-handler") }
        assertThat(http.getBody("/test-future")).isEqualTo("Overwritten by after-handler")
    }

    @Test
    fun `error-handlers run after future is resolved`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.status(555).future(getFuture("Not result")) }
        app.error(555) { ctx -> ctx.result("Overwritten by error-handler") }
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
    fun `calling future in (before - get - after) handlers works`() = TestUtil.test { app, http ->
        app.before("/future") { it.future(getFuture("before")) }
        app.get("/future") { it.future(getFuture("nothing")) { /* do nothing */ } }
        app.after("/future") { it.future(getFuture("${it.resultString()}, after")) }
        assertThat(http.get("/future").body).isEqualTo("before, after")
    }

    @Test
    fun `calling future in an exception-handler throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { throw Exception() }
        app.exception(Exception::class.java) { _, ctx -> ctx.future(getFuture("Not result")) }
        assertThat(http.getBody("/test-future")).isEqualTo("")
        assertThat(http.get("/test-future").status).isEqualTo(500)
    }

    /* Such a scenario doesn't really make sense, the future may perform various side effect tasks - not only set the result response.
     * Despite the fact it doesn't make sense, this operation is also not thread-safe, because of the mutable nature of Context.
     * Example:
     *
     * task = externalThreadPool.task {
     *   ctx.result("Overridden") // There is a chance it will be called before async context initialization because 'result' may override async task
     *   ctx.status(404)          // May fail, as context was handled as sync and has been already sent
     * }
     *
     * ctx.future(task)
     */
    @Test
    @Disabled
    fun `future is overwritten if String result is set`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx ->
            ctx.future(getFuture("Result"))
            ctx.result("Overridden")
        }
        assertThat(http.getBody("/test-future")).isEqualTo("Overridden")
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
        assertThat(noContentResponse.body).isEqualTo(null)
        assertThat(noContentResponse.headers.getFirst(Header.CONTENT_TYPE)).isEqualTo(ContentType.PLAIN)
    }

    private val impatientServer: Javalin by lazy { Javalin.create { it.asyncRequestTimeout = 5 } }

    @Test
    fun `default timeout error isn't jetty branded`() = TestUtil.test(impatientServer) { app, http ->
        app.get("/") { it.future(getFuture("Test", delay = 50)) }
        assertThat(http.get("/").body).isEqualTo("Request timed out")
    }

    @Test
    fun `can override timeout with custom error message`() = TestUtil.test(impatientServer) { app, http ->
        app.get("/") { it.future(getFuture("Test", delay = 50)) }
        app.error(500) { ctx -> ctx.result("My own simple error message") }
        assertThat(http.get("/").body).isEqualTo("My own simple error message")
    }

    @Test
    fun `timed out futures are canceled`() = TestUtil.test(impatientServer) { app, http ->
        val future = CompletableFuture<String>()
        app.get("/") {
            Executors.newSingleThreadScheduledExecutor().schedule({
                future.complete("Test")
            }, 500, TimeUnit.MILLISECONDS)
            it.future(future)
        }
        assertThat(http.get("/").body).isEqualTo("Request timed out")
        assertThat(future.isCancelled).isTrue()
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
        return CompletableFuture.supplyAsync({ throw failure })
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

