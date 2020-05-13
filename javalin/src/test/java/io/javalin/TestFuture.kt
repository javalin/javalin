package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TestFuture {

    @Test
    fun `hello future world`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.result(getFuture("Result")) }
        assertThat(http.getBody("/test-future")).isEqualTo("Result")
    }

    @Test
    fun `hello future world json`() = TestUtil.test { app, http ->
        app.get("/test-future-json") { ctx -> ctx.json(getFuture("JSON result")) }
        assertThat(http.getBody("/test-future-json")).isEqualTo("\"JSON result\"")
    }

    @Test
    fun `after-handlers run after future is resolved`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.result(getFuture("Not result")) }
        app.after { ctx -> ctx.result("Overwritten by after-handler") }
        assertThat(http.getBody("/test-future")).isEqualTo("Overwritten by after-handler")
    }

    @Test
    fun `setting future in after-handler throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.result(getFuture("Not result")) }
        app.after("/test-future") { ctx -> ctx.result(getFuture("Overwritten by after-handler")) }
        assertThat(http.getBody("/test-future")).isEqualTo("Internal server error")
    }

    @Test
    fun `error-handlers run after future is resolved`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.result(getFuture("Not result")).status(555) }
        app.error(555) { ctx -> ctx.result("Overwritten by error-handler") }
        assertThat(http.getBody("/test-future")).isEqualTo("Overwritten by error-handler")
    }

    @Test
    fun `unresolved future throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.result(getFuture(null)) }
        assertThat(http.getBody("/test-future")).isEqualTo("Internal server error")
    }

    @Test
    fun `future throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.result(getFuture(null)) }
        assertThat(http.getBody("/test-future")).isEqualTo("Internal server error")
    }

    @Test
    fun `unresolved futures are handled by exception-mapper`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.result(getFuture(null)) }
        app.exception(CancellationException::class.java) { _, ctx -> ctx.result("Handled") }
        assertThat(http.getBody("/test-future")).isEqualTo("Handled")
    }

    @Test
    fun `futures failures are handled by exception-mapper`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.json(getFailingFuture(UnsupportedOperationException())) }
        app.exception(UnsupportedOperationException::class.java) { _, ctx -> ctx.result("Handled") }
        assertThat(http.getBody("/test-future")).isEqualTo("Handled")
    }

    @Test
    fun `setting a future in an exception-handler throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { throw Exception() }
        app.exception(Exception::class.java) { _, ctx -> ctx.result(getFuture("Exception result")) }
        assertThat(http.getBody("/test-future")).isEqualTo("")
        assertThat(http.get("/test-future").status).isEqualTo(500)
    }

    @Test
    fun `future is overwritten if String result is set`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx ->
            ctx.result(getFuture("Result"))
            ctx.result("Overridden")
        }
        assertThat(http.getBody("/test-future")).isEqualTo("Overridden")
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

}

