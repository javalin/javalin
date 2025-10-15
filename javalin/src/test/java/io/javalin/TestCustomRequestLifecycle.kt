package io.javalin

import io.javalin.http.servlet.DefaultTasks.HTTP
import io.javalin.http.servlet.SubmitOrder.LAST
import io.javalin.http.servlet.Task
import io.javalin.http.servlet.TaskInitializer
import io.javalin.testing.TestUtil
import io.javalin.testing.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestCustomRequestLifecycle {

    @Test
    fun `can remove lifecycle stage`() = TestUtil.test(Javalin.create { config ->
        config.pvt.servletRequestLifecycle = mutableListOf(HTTP)
        config.routes.before { it.result("Overridden") }
        config.routes.get("/") { it.result("Hello!") }
        config.routes.after { it.result("Overridden") }
    }) { app, http ->
        assertThat(http.getBody("/")).isEqualTo("Hello!")
    }

    @Test
    fun `can add custom lifecycle stage`() = TestUtil.test(Javalin.create { config ->
        config.pvt.servletRequestLifecycle = mutableListOf(
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
        app.before {
            it.result("Before")
            it.skipRemainingHandlers()
        }
        app.get("/") { it.result("Http") }
        app.after { it.result("After") }
        assertThat(http.getBody("/")).isEqualTo("Before")
    }

}
