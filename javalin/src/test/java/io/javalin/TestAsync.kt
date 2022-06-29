package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TestAsync {

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
