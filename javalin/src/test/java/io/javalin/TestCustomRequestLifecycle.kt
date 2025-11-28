package io.javalin

import io.javalin.http.Context
import io.javalin.http.servlet.DefaultTasks.HTTP
import io.javalin.http.servlet.SubmitOrder.LAST
import io.javalin.http.servlet.Task
import io.javalin.http.servlet.TaskInitializer
import io.javalin.testing.TestUtil

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestCustomRequestLifecycle {

    private fun Context.accumulatingResult(s: String) = this.result((result() ?: "") + s)

    @Test
    fun `can remove lifecycle stage`() = TestUtil.test(Javalin.create { config ->
        config.requestLifeCycle(HTTP)
        config.routes.before { it.accumulatingResult("Overridden") }
        config.routes.get("/") { it.accumulatingResult("Hello!") }
        config.routes.after { it.accumulatingResult("Overridden") }
    }) { app, http ->
        assertThat(http.getBody("/")).isEqualTo("Hello!")
    }

    @Test
    fun `can add custom lifecycle stage`() = TestUtil.test(Javalin.create { config ->
        config.requestLifeCycle(
            HTTP,
            TaskInitializer { submitTask, _, ctx, _ ->
                submitTask(LAST, Task {
                    ctx.result("Static after!")
                })
            }
        )
        config.routes.get("/") { it.result("Hello!") }
    }) { app, http ->
        assertThat(http.getBody("/")).isEqualTo("Static after!")
    }

    @Test
    fun `can terminate request handling`() = TestUtil.test { app, http ->
        app.unsafe.routes.before {
            it.result("Before")
            it.skipRemainingHandlers()
        }
        app.unsafe.routes.get("/") { it.result("Http") }
        app.unsafe.routes.after { it.result("After") }
        assertThat(http.getBody("/")).isEqualTo("Before")
    }

}
