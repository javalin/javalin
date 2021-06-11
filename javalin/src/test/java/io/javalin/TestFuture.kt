package io.javalin

import com.mashape.unirest.http.exceptions.UnirestException
import io.javalin.core.util.Header
import io.javalin.testing.SerializeableObject
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TestFuture {

    @Test
    fun `hello future world`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.async().result(getFuture("Result")) }
        assertThat(http.getBody("/test-future")).isEqualTo("Result")
    }

    @Test
    fun `after-handlers run after future is resolved`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.async().result(getFuture("Not result")) }
        app.after { ctx -> ctx.result("Overwritten by after-handler") }
        assertThat(http.getBody("/test-future")).isEqualTo("Overwritten by after-handler")
    }

    @Test
    fun `calling async in after-handler throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.async().result(getFuture("Not result")) }
        app.after("/test-future") { ctx -> ctx.async() }
        assertThat(http.getBody("/test-future")).isEqualTo("Internal server error")
    }

    @Test
    fun `error-handlers run after future is resolved`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.status(555).async().result(getFuture("Not result")) }
        app.error(555) { ctx -> ctx.result("Overwritten by error-handler") }
        assertThat(http.getBody("/test-future")).isEqualTo("Overwritten by error-handler")
    }

    @Test
    fun `unresolved future throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.async().result(getFuture(null)) }
        assertThat(http.getBody("/test-future")).isEqualTo("Internal server error")
    }

    @Test
    fun `unresolved futures are handled by exception-mapper`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.async().result(getFuture(null)) }
        app.exception(CancellationException::class.java) { _, ctx -> ctx.result("Handled") }
        assertThat(http.getBody("/test-future")).isEqualTo("Handled")
    }

    @Test
    fun `futures failures are handled by exception-mapper`() = TestUtil.test { app, http ->
        app.get("/test-future") { it.async().json(getFailingFuture(UnsupportedOperationException())) }
        app.exception(UnsupportedOperationException::class.java) { _, ctx -> ctx.result("Handled") }
        assertThat(http.getBody("/test-future")).isEqualTo("Handled")
    }

    @Test
    fun `calling async in an exception-handler throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> throw Exception() }
        app.exception(Exception::class.java) { _, ctx -> ctx.async() }
        assertThat(http.getBody("/test-future")).isEqualTo("")
        assertThat(http.get("/test-future").status).isEqualTo(500)
    }

    @Test
    fun `future is overwritten if String result is set`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx ->
            ctx.async().result(getFuture("Result"))
            ctx.result("Overridden")
        }
        assertThat(http.getBody("/test-future")).isEqualTo("Overridden")
    }

    @Test
    fun `future throws when stream throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx ->
            ctx.async().result(getFutureFailingStream())
        }
        try {
            assertThat(http.get("/test-future").status).isEqualTo(500)
        } catch (e: UnirestException) { // We need to catch Unirest's exception, as TestUtil swallows it
            fail("Unirest is not supposed to throw", e)
        }
    }

    @Test
    fun `loonyrunes is happy with the api`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val noContent = ctx.queryParam("no-content") != null
            ctx.status(404) // should never happen
            val asyncContext = ctx.async()
            if (noContent) {
                ctx.status(204)
            } else {
                ctx.status(200)
                asyncContext.json(getFuture("My future"))
            }
        }
        val contentResponse = http.get("/")
        assertThat(contentResponse.status).isEqualTo(200)
        assertThat(contentResponse.body).isEqualTo("""My future""")
        assertThat(contentResponse.headers.getFirst(Header.CONTENT_TYPE)).isEqualTo("application/json")
        val noContentResponse = http.get("/?no-content")
        assertThat(noContentResponse.status).isEqualTo(204)
        assertThat(noContentResponse.body).isEqualTo(null)
        assertThat(noContentResponse.headers.getFirst(Header.CONTENT_TYPE)).isEqualTo("text/plain")
    }

    private fun getFuture(result: String?): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        Executors.newSingleThreadScheduledExecutor().schedule({
            if (result != null) {
                future.complete(result)
            } else {
                future.cancel(false)
            }
        }, 10, TimeUnit.MILLISECONDS)
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

