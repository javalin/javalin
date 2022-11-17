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
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpStatus.UNAUTHORIZED
import io.javalin.security.RouteRole
import io.javalin.testing.TestUtil
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class TestAccessManager {

    enum class MyRoles : RouteRole { ROLE_ONE, ROLE_TWO, ROLE_THREE }

    private fun managedApp() = Javalin.create { config ->
        config.accessManager { handler, ctx, routeRoles ->
            val role: RouteRole? = ctx.queryParam("role")?.let { MyRoles.valueOf(it) }

            when (role) {
                in routeRoles -> handler.handle(ctx)
                else -> ctx.status(UNAUTHORIZED).result(UNAUTHORIZED.message)
            }
        }
    }

    @Test
    fun `default AccessManager throws if roles are present`() = TestUtil.test { app, http ->
        app.get("/secured", { it.result("Hello") }, ROLE_ONE)
        assertThat(callWithRole(http.origin, "/secured", "ROLE_ONE")).isEqualTo(INTERNAL_SERVER_ERROR.message)
    }

    @Test
    fun `AccessManager can restrict access for instance`() = TestUtil.test(managedApp()) { app, http ->
        app.get("/secured", { it.result("Hello") }, ROLE_ONE, ROLE_TWO)
        assertThat(callWithRole(http.origin, "/secured", "ROLE_ONE")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/secured", "ROLE_TWO")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/secured", "ROLE_THREE")).isEqualTo(UNAUTHORIZED.message)
    }

    @Test
    fun `AccessManager can restrict access for ApiBuilder`() = TestUtil.test(managedApp()) { app, http ->
        app.routes {
            get("/static-secured", { it.result("Hello") }, ROLE_ONE, ROLE_TWO)
        }
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_ONE")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_TWO")).isEqualTo("Hello")
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_THREE")).isEqualTo(UNAUTHORIZED.message)
    }

    @Test
    fun `AccessManager can restrict access for ApiBuilder crud`() = TestUtil.test(managedApp()) { app, http ->
        app.routes {
            crud("/users/{userId}", TestApiBuilder.UserController(), ROLE_ONE, ROLE_TWO)
        }
        assertThat(callWithRole(http.origin, "/users/1", "ROLE_ONE")).isEqualTo("My single user: 1")
        assertThat(callWithRole(http.origin, "/users/2", "ROLE_TWO")).isEqualTo("My single user: 2")
        assertThat(callWithRole(http.origin, "/users/3", "ROLE_THREE")).isEqualTo(UNAUTHORIZED.message)
    }

    @Test
    fun `AccessManager supports path params`() = TestUtil.test(Javalin.create {
        it.accessManager { _, ctx, _ ->
            ctx.result(ctx.pathParam("userId"));
        }
    }) { app, http ->
        app.get("/user/{userId}", {}, ROLE_ONE)
        assertThat(http.get("/user/123").body).isEqualTo("123")
    }

    @Test
    fun `AccessManager is handled as standalone layer by servlet`() = TestUtil.test(Javalin.create {
        it.accessManager { handler, ctx, _ ->
            ctx.future { CompletableFuture.completedFuture("Something async") }
            handler.handle(ctx) // it shouldn't override values from current layer (like future supplier)
        }
    }) { app, http ->
        app.get("/secured", { ctx ->
            ctx.async {
               ctx.result("Test")
            }
        }, ROLE_ONE)
        assertThat(callWithRole(http.origin, "/secured", "ROLE_ONE")).isEqualTo("Test")
    }

    private fun callWithRole(origin: String, path: String, role: String) =
        Unirest.get(origin + path).queryString("role", role).asString().body

}
