package io.javalin

import io.javalin.http.servlet.DefaultTasks.HTTP
import io.javalin.http.servlet.JavalinServletContext
import io.javalin.http.servlet.SubmitOrder.LAST
import io.javalin.http.servlet.Task
import io.javalin.http.servlet.TaskInitializer
import io.javalin.security.AccessManagerState.INVOKED
import io.javalin.security.AccessManagerState.PASSED
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestCustomRequestLifecycle {

    @Test
    fun `can remove lifecycle stage`() = TestUtil.test(Javalin.create {
        it.pvt.servletRequestLifecycle = listOf(HTTP)
    }) { app, http ->
        app.before { it.result("Overridden") }
        app.get("/") { it.result("Hello!") }
        app.after { it.result("Overridden") }
        assertThat(http.getBody("/")).isEqualTo("Hello!")
    }

    @Test
    fun `can add custom lifecycle stage`() = TestUtil.test(Javalin.create {
        it.pvt.servletRequestLifecycle = listOf(
            HTTP,
            TaskInitializer {
                it.submitTask(LAST, Task {
                    it.ctx.result("Static after!")
                })
            }
        )
    }) { app, http ->
        app.get("/") { it.result("Hello!") }
        assertThat(http.getBody("/")).isEqualTo("Static after!")
    }

    @Test
    fun `can terminate request handling using unsafe api`() = TestUtil.test { app, http ->
        app.before {
            it.result("Before")
            (it as JavalinServletContext).tasks.clear()
        }
        app.get("/") { it.result("Http") }
        app.after { it.result("After") }
        assertThat(http.getBody("/")).isEqualTo("Before")
    }

    @Test
    fun `gh-1858 can add lifecycle stage between access manager and http handler`() = TestUtil.test(Javalin.create { config ->
        config.accessManager { handler, ctx, _ ->
            handler.handle(ctx) // authenticated
        }
        config.pvt.servletRequestLifecycle = config.pvt.servletRequestLifecycle.toMutableList().also {
            it.add(it.indexOf(HTTP), TaskInitializer { initializerContext ->
                initializerContext.submitTask(LAST, Task {
                    if (initializerContext.accessManagerState == PASSED) {
                        initializerContext.ctx.result("Cached response")
                        initializerContext.accessManagerState = INVOKED // prevent http layer from being called
                    }
                })
            })
        }
    }) { app, http ->
        app.get("/") { it.result("Hello!") }
        assertThat(http.getBody("/")).isEqualTo("Cached response")
    }

}
