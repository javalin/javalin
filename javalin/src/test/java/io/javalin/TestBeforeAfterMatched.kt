package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestBeforeAfterMatched {

    @Test
    fun `beforeMatched and afterMatched work`() = TestUtil.test { app, http ->
        app.before { ctx ->
            ctx.result("foo")
        }
        app.beforeMatched { ctx ->
            ctx.result("before-matched")
        }
        app.get("/hello") { ctx ->
            ctx.result(ctx.result() + "-hello")
        }
        app.afterMatched { ctx ->
            ctx.result(ctx.result() + "-after-matched")
        }
        app.after { ctx ->
            ctx.result(ctx.result() + "!")
        }

        assertThat(http.getBody("/other-path")).isEqualToIgnoringCase("Not Found!")
        assertThat(http.getBody("/hello")).isEqualTo("before-matched-hello-after-matched!")
    }

    @Test
    fun `beforeMatched can skip remaining handlers`() = TestUtil.test { app, http ->
        app.beforeMatched {
            it.result("static-before")
            it.skipRemainingHandlers()
        }
        app.get("/hello") {
            it.result("hello")
        }
        assertThat(http.getBody("/other-path")).isEqualToIgnoringCase("Not found")
        assertThat(http.getBody("/hello")).isEqualTo("static-before")
    }

    // TODO: check singlePageHandler, ResourceHandler, head request on get handler
}
