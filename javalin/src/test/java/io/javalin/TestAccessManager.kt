/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.TestAccessManager.MyRole.ROLE_ONE
import io.javalin.TestAccessManager.MyRole.ROLE_TWO
import io.javalin.apibuilder.ApiBuilder.crud
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.config.JavalinConfig
import io.javalin.http.HttpStatus.UNAUTHORIZED
import io.javalin.http.UnauthorizedResponse
import io.javalin.security.RouteRole
import io.javalin.testing.TestUtil
import io.javalin.testing.UnirestReplacement as Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestAccessManager {

    // the AccessManager interface has been removed, but we are keeping
    // the test to make sure the functionality is still supported.

    enum class MyRole : RouteRole { ROLE_ONE, ROLE_TWO, ROLE_THREE }

    private fun managedApp(cfg: ((JavalinConfig) -> Unit)? = null) = Javalin.create { config ->
        config.router.mount {
            it.beforeMatched { ctx ->
                val role: RouteRole? = ctx.queryParam("role")?.let { MyRole.valueOf(it) }
                val routeRoles = ctx.routeRoles()
                if (role !in routeRoles) {
                    throw UnauthorizedResponse()
                }
            }
        }
        cfg?.invoke(config)
    }

    @Test
    fun `AccessManager can restrict access for instance`() = TestUtil.test(managedApp()) { app, http ->
        app.get("/secured", { it.result("Hello") }, ROLE_ONE, ROLE_TWO)
        assertThat(callWithRole(http.origin, "/secured", "ROLE_ONE")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/secured", "ROLE_TWO")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/secured", "ROLE_THREE")).isEqualTo(UNAUTHORIZED.message)
    }

    @Test
    fun `AccessManager can restrict access for ApiBuilder`() = TestUtil.test(managedApp { cfg ->
        cfg.router.apiBuilder {
            get("/static-secured", { it.result("Hello") }, ROLE_ONE, ROLE_TWO)
        }
    }) { app, http ->
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_ONE")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_TWO")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_THREE")).isEqualTo(UNAUTHORIZED.message)
    }

    @Test
    fun `AccessManager can restrict access for ApiBuilder crud`() = TestUtil.test(managedApp { cfg ->
        cfg.router.apiBuilder {
            path("/users/{userId}") {
                crud(TestApiBuilder.UserController(), ROLE_ONE, ROLE_TWO)
            }
        }
    }) { app, http ->
        assertThat(callWithRole(http.origin, "/users/1", "ROLE_ONE")).isEqualTo("My single user: 1")
        assertThat(callWithRole(http.origin, "/users/2", "ROLE_TWO")).isEqualTo("My single user: 2")
        assertThat(callWithRole(http.origin, "/users/3", "ROLE_THREE")).isEqualTo(UNAUTHORIZED.message)
    }

    private fun callWithRole(origin: String, path: String, role: String) =
        Unirest.get(origin + path).queryString("role", role).asString().body

}
