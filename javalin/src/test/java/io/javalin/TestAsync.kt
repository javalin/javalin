package io.javalin

import io.javalin.http.HttpCode.ENHANCE_YOUR_CALM
import io.javalin.http.HttpCode.IM_A_TEAPOT
import io.javalin.http.HttpCode.OK
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
    fun `async tasks should start execution in a proper order`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            ctx.async {
                ctx.async {
                    ctx.status(OK)
                }
                Thread.sleep(100)
                ctx.status(ENHANCE_YOUR_CALM)
            }
            Thread.sleep(100)
            ctx.status(IM_A_TEAPOT)
        }

        assertThat(http.get("/").status).isEqualTo(OK.status)
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
