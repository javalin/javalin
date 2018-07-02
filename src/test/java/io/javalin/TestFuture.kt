package io.javalin

import io.javalin.util.BaseTest
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat

class TestFuture : BaseTest() {

    @Test
    fun testFutures() {
        app.get("/test-future") { ctx -> ctx.result(getFuture("Result")) }
        assertThat(http.getBody("/test-future"), `is`("Result"))
    }

    @Test
    fun testFutures_afterHandler() {
        app.get("/test-future") { ctx -> ctx.result(getFuture("Not result")) }
        app.after { ctx -> ctx.result("Overwritten by after-handler") }
        assertThat(http.getBody("/test-future"), `is`("Overwritten by after-handler"))
    }

    @Test
    fun testFutures_afterHandler_throwsExceptionForFuture() {
        app.get("/test-future") { ctx -> ctx.result(getFuture("Not result")) }
        app.after("/test-future") { ctx -> ctx.result(getFuture("Overwritten by after-handler")) }
        assertThat(http.getBody("/test-future"), `is`("Internal server error"))
    }

    @Test
    fun testFutures_errorHandler() {
        app.get("/test-future") { ctx -> ctx.result(getFuture("Not result")).status(555) }
        app.error(555) { ctx -> ctx.result("Overwritten by error-handler") }
        assertThat(http.getBody("/test-future"), `is`("Overwritten by error-handler"))
    }

    @Test
    fun testFutures_exceptionalFutures_unmapped() {
        app.get("/test-future") { ctx -> ctx.result(getFuture(null)) }
        assertThat(http.getBody("/test-future"), `is`("Internal server error"))
    }

    @Test
    fun testFutures_exceptionalFutures_mapped() {
        app.get("/test-future") { ctx -> ctx.result(getFuture(null)) }
        app.exception(CancellationException::class.java) { e, ctx -> ctx.result("Handled") }
        assertThat(http.getBody("/test-future"), `is`("Handled"))
    }

    @Test
    fun testFutures_futureInExceptionHandler_throwsException() {
        app.get("/test-future") { ctx -> throw Exception() }
        app.exception(Exception::class.java) { exception, ctx -> ctx.result(getFuture("Exception result")) }
        assertThat(http.getBody("/test-future"), `is`(""))
        assertThat(http.get("/test-future").code(), `is`(500))
    }

    @Test
    fun testFutures_clearedOnNewResult() {
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
