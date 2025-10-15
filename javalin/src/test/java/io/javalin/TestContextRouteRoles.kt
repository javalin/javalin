package io.javalin

import io.javalin.security.RouteRole
import io.javalin.testing.TestUtil
import org.junit.jupiter.api.Test
import io.javalin.Role.A
import io.javalin.Role.B
import io.javalin.Role.C
import io.javalin.Role.D
import io.javalin.http.Context
import io.javalin.testing.*
import org.assertj.core.api.Assertions.assertThat

enum class Role : RouteRole { A, B, C, D }

class TestContextRouteRoles {

    private fun Context.sortedRoles() = this.routeRoles().sortedBy { it.toString() }

    @Test
    fun `route roles are available in http handler`() = TestUtil.test { app, http ->
        app.get("/roles", { it.result("${it.sortedRoles()}") }, A, B, C)
        http.get("/roles").let { assertThat(it.body).isEqualTo("[A, B, C]") }
        app.get("/no-roles") { it.result("${it.sortedRoles()}") }
        http.get("/no-roles").let { assertThat(it.body).isEqualTo("[]") }
    }

    @Test
    fun `route roles are available in beforeMatched`() = TestUtil.test { app, http ->
        app.beforeMatched { it.result("${it.sortedRoles()}") }
        app.get("/roles", { it.result(it.result() + "!!!") }, A, D)
        http.get("/roles").let { assertThat(it.body).isEqualTo("[A, D]!!!") }
    }

    @Test
    fun `route roles are NOT available in before`() = TestUtil.test { app, http ->
        app.before { it.result("${it.sortedRoles()}") }
        app.get("/roles", { it.result(it.result() + "!!!") }, C, D)
        http.get("/roles").let { assertThat(it.body).isEqualTo("[]!!!") }
    }

    @Test
    fun `route roles are available in afterMatched`() = TestUtil.test { app, http ->
        app.get("/roles", { it.result("!!!") }, B)
        app.afterMatched { it.result(it.result() + it.sortedRoles()) }
        http.get("/roles").let { assertThat(it.body).isEqualTo("!!![B]") }
    }

    @Test
    fun `route roles are available in after`() = TestUtil.test { app, http ->
        // the roles were set in the endpoint handler, so they are available in after too
        app.get("/roles", { it.result("!!!") }, B, C, D)
        app.after { it.result(it.result() + it.sortedRoles()) }
        http.get("/roles").let { assertThat(it.body).isEqualTo("!!![B, C, D]") }
    }

}
