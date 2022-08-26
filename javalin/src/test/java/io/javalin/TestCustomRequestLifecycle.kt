package io.javalin

import io.javalin.http.Stage
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestCustomRequestLifecycle {

    @Test
    fun `can remove lifecycle stage`() = TestUtil.test { app, http ->
        app.javalinServlet().lifecycle.removeIf { it.id == "after" }
        app.get("/") { it.result("Hello!") }
        app.after { it.result("Overridden") }
        assertThat(http.getBody("/")).isEqualTo("Hello!")
    }

    @Test
    fun `can add custom lifecycle stage`() = TestUtil.test { app, http ->
        app.javalinServlet().lifecycle.removeIf { it.id == "after" }
        app.javalinServlet().lifecycle.add(Stage("static-after", skipTasksOnException = false) { submitTask ->
            submitTask {
                ctx.result("Static after!")
            }
        })
        app.get("/") { it.result("Hello!") }
        assertThat(http.getBody("/")).isEqualTo("Static after!")
    }

}
