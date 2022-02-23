package io.javalin

import io.javalin.http.DefaultName
import io.javalin.http.Stage
import io.javalin.http.StageName
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestCustomRequestLifecycle {

    @Test
    fun `can remove lifecycle stage`() = TestUtil.test { app, http ->
        app.javalinServlet().lifecycle.removeIf { it.name == DefaultName.AFTER }
        app.get("/") { ctx -> ctx.result("Hello!") }
        app.after { ctx -> ctx.result("Overridden") }
        assertThat(http.getBody("/")).isEqualTo("Hello!")
    }

    enum class CustomName : StageName { CustomAfter }

    @Test
    fun `can add custom lifecycle stage`() = TestUtil.test { app, http ->
        app.javalinServlet().lifecycle.removeIf { it.name == DefaultName.AFTER }
        app.javalinServlet().lifecycle.add(Stage(CustomName.CustomAfter) {
            ctx.result("Static after!")
        })
        app.get("/") { ctx -> ctx.result("Hello!") }
        assertThat(http.getBody("/")).isEqualTo("Static after!")
    }

}
