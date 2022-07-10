/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.TestAccessManager.MyRoles.ROLE_ONE
import io.javalin.TestAccessManager.MyRoles.ROLE_TWO
import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.security.RouteRole
import io.javalin.testing.TestUtil
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestAccessManager {

    enum class MyRoles : io.javalin.security.RouteRole { ROLE_ONE, ROLE_TWO, ROLE_THREE }

    private fun managedApp() = Javalin.create { config ->
        config.core.accessManager { handler, ctx, routeRoles ->
            val userRole = ctx.queryParam("role")
            if (userRole != null && MyRoles.valueOf(userRole) in routeRoles) {
                handler.handle(ctx)
            } else {
                ctx.status(401).result("Unauthorized")
            }
        }
    }

    @Test
    fun `default AccessManager throws if roles are present`() = TestUtil.test { app, http ->
        app.get("/secured", { it.result("Hello") }, ROLE_ONE)
        assertThat(callWithRole(http.origin, "/secured", "ROLE_ONE")).isEqualTo("Internal server error")
    }

    @Test
    fun `AccessManager can restrict access for instance`() = TestUtil.test(managedApp()) { app, http ->
        app.get("/secured", { it.result("Hello") }, ROLE_ONE, ROLE_TWO)
        assertThat(callWithRole(http.origin, "/secured", "ROLE_ONE")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/secured", "ROLE_TWO")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/secured", "ROLE_THREE")).isEqualTo("Unauthorized")
    }

    @Test
    fun `AccessManager can restrict access for ApiBuilder`() = TestUtil.test(managedApp()) { app, http ->
        app.routes {
            get("/static-secured", { it.result("Hello") }, ROLE_ONE, ROLE_TWO)
        }
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_ONE")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_TWO")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_THREE")).isEqualTo("Unauthorized")
    }

    @Test
    fun `AccessManager can restrict access for ApiBuilder crud`() = TestUtil.test(managedApp()) { app, http ->
        app.routes {
            crud("/users/{userId}", TestApiBuilder.UserController(), ROLE_ONE, ROLE_TWO)
        }
        assertThat(callWithRole(http.origin, "/users/1", "ROLE_ONE")).isEqualTo("My single user: 1")
        assertThat(callWithRole(http.origin, "/users/2", "ROLE_TWO")).isEqualTo("My single user: 2")
        assertThat(callWithRole(http.origin, "/users/3", "ROLE_THREE")).isEqualTo("Unauthorized")
    }

    private fun callWithRole(origin: String, path: String, role: String) =
        Unirest.get(origin + path).queryString("role", role).asString().body

}
