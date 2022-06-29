package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TestAsync {

    @Test
    fun `async requests works`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            ctx.async {
                ctx.result("Response")
            }
        }

        assertThat(http.get("/").body).isEqualTo("Response")
    }

    @Test
    fun `exception in async works`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            ctx.async {
                throw UnsupportedOperationException()
            }
        }

        assertThat(http.get("/").body).isEqualTo("Internal server error")
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
