package io.javalin

import io.javalin.http.ContentType
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

internal class TestResult {

    @Test
    fun `should respond with specified input-stream `() = TestUtil.test { app, http ->
        app.get("/") { it.future(completedFuture("Response".byteInputStream()), callback = {}) }
        assertThat(http.getBody("/")).isEqualTo("Response")
    }

    @Test
    fun `result-stream fetches valid value from future`() = TestUtil.test { app, http ->
        app.get("/works") {
            val stream = "Works".byteInputStream()
            it.future(completedFuture(stream))
            assertThat(it.resultStream()).isEqualTo(stream)
        }
        assertThat(http.getBody("/works")).isEqualTo("Works")

        app.get("/cancelled") {
            it.future(CompletableFuture<Any?>().also { future -> future.cancel(true) })
            assertDoesNotThrow { assertThat(it.resultStream()).isNull() }
        }
        assertThat(http.getBody("/cancelled")).isEqualTo(INTERNAL_SERVER_ERROR.message)

        app.get("/errored") {
            it.future(CompletableFuture.failedFuture<Any?>(UnsupportedOperationException()))
            assertDoesNotThrow { assertThat(it.resultStream()).isNull() }
        }
        assertThat(http.getBody("/errored")).isEqualTo(INTERNAL_SERVER_ERROR.message)
    }

    @Test
    fun `result function can be called once per handler`() = TestUtil.test { app, http ->
        app.before("/") { ctx ->
            ctx.future(completedFuture("Before")) {
                ctx.future(completedFuture("$it Callback"))
            }
        }
        app.get("/") { ctx ->
            ctx.future(completedFuture("${ctx.resultString()} Http")) {
                ctx.future(completedFuture("$it Callback"))
            }
        }
        app.after("/") { ctx ->
            ctx.future(completedFuture("${ctx.resultString()} After")) {
                ctx.future(completedFuture("$it Callback"))
            }
        }
        assertThat(http.getBody("/")).isEqualTo("Before Callback Http Callback After Callback")
    }

    @Test
    fun `context should throw in case of multiple result function calls`() = TestUtil.test { app, http ->
        app.get("/") {
            it.result("Success")
            assertThrows<IllegalStateException> { it.result("Text") }
            assertThrows<IllegalStateException> { it.result("Text".toByteArray()) }
            assertThrows<IllegalStateException> { it.result("Text".byteInputStream()) }
            assertThrows<IllegalStateException> { it.writeSeekableStream("Text".byteInputStream(), ContentType.OCTET_STREAM) }
            assertThrows<IllegalStateException> { it.future(completedFuture("Future")) }
            assertThrows<IllegalStateException> { it.async {} }
        }
        assertThat(http.getBody("/")).isEqualTo("Success")
    }

}
