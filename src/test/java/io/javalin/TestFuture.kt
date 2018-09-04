package io.javalin

import io.javalin.util.TestUtil
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TestFuture {

    @Test
    fun `hello future world`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.result(getFuture("Result")) }
        assertThat(http.getBody("/test-future"), `is`("Result"))
    }

    @Test
    fun `hello future world json`() = TestUtil.test { app, http ->
        app.get("/test-future-json") { ctx -> ctx.json(getFuture("JSON result")) }
        assertThat(http.getBody("/test-future-json"), `is`("\"JSON result\""))
    }

    @Test
    fun `after-handlers run after future is resolved`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.result(getFuture("Not result")) }
        app.after { ctx -> ctx.result("Overwritten by after-handler") }
        assertThat(http.getBody("/test-future"), `is`("Overwritten by after-handler"))
    }

    @Test
    fun `setting future in after-handler throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.result(getFuture("Not result")) }
        app.after("/test-future") { ctx -> ctx.result(getFuture("Overwritten by after-handler")) }
        assertThat(http.getBody("/test-future"), `is`("Internal server error"))
    }

    @Test
    fun `error-handlers run after future is resolved`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.result(getFuture("Not result")).status(555) }
        app.error(555) { ctx -> ctx.result("Overwritten by error-handler") }
        assertThat(http.getBody("/test-future"), `is`("Overwritten by error-handler"))
    }

    @Test
    fun `unresolved future throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.result(getFuture(null)) }
        assertThat(http.getBody("/test-future"), `is`("Internal server error"))
    }

    @Test
    fun `unresolved futures are handled by exception-mapper`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> ctx.result(getFuture(null)) }
        app.exception(CancellationException::class.java) { e, ctx -> ctx.result("Handled") }
        assertThat(http.getBody("/test-future"), `is`("Handled"))
    }

    @Test
    fun `setting a future in an exception-handler throws`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx -> throw Exception() }
        app.exception(Exception::class.java) { exception, ctx -> ctx.result(getFuture("Exception result")) }
        assertThat(http.getBody("/test-future"), `is`(""))
        assertThat(http.get("/test-future").status, `is`(500))
    }

    @Test
    fun `future is overwritten if String result is set`() = TestUtil.test { app, http ->
        app.get("/test-future") { ctx ->
            ctx.result(getFuture("Result"))
            ctx.result("Overridden")
        }
        assertThat(http.getBody("/test-future"), `is`("Overridden"))
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

}
