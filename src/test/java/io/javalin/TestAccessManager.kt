/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.TestAccessManager.MyRoles.ROLE_ONE
import io.javalin.TestAccessManager.MyRoles.ROLE_TWO
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.security.AccessManager
import io.javalin.security.Role
import io.javalin.security.SecurityUtil.roles
import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestAccessManager {

    enum class MyRoles : Role { ROLE_ONE, ROLE_TWO, ROLE_THREE }

    private val accessManager = AccessManager { handler, ctx, permittedRoles ->
        val userRole = ctx.queryParam("role")
        if (userRole != null && permittedRoles.contains(MyRoles.valueOf(userRole))) {
            handler.handle(ctx)
        } else {
            ctx.status(401).result("Unauthorized")
        }
    }

    @Test
    fun `default AccessManager throws if roles are present`() = TestUtil.test { app, http ->
        app.get("/secured", { ctx -> ctx.result("Hello") }, roles(ROLE_ONE))
        assertThat(callWithRole(http.origin, "/secured", "ROLE_ONE"), `is`("Internal server error"))
    }

    @Test
    fun `AccessManager can restrict access for instance`() = TestUtil.test { app, http ->
        app.accessManager(accessManager)
        app.get("/secured", { ctx -> ctx.result("Hello") }, roles(ROLE_ONE, ROLE_TWO))
        assertThat(callWithRole(http.origin, "/secured", "ROLE_ONE"), `is`("Hello"))
        assertThat(callWithRole(http.origin, "/secured", "ROLE_TWO"), `is`("Hello"))
        assertThat(callWithRole(http.origin, "/secured", "ROLE_THREE"), `is`("Unauthorized"))
    }

    @Test
    fun `AccessManager can restrict access for ApiBuilder`() = TestUtil.test { app, http ->
        app.accessManager(accessManager)
        app.routes {
            get("/static-secured", { ctx -> ctx.result("Hello") }, roles(ROLE_ONE, ROLE_TWO))
        }
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_ONE"), `is`("Hello"))
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_TWO"), `is`("Hello"))
        assertThat(callWithRole(http.origin, "/static-secured", "ROLE_THREE"), `is`("Unauthorized"))
    }

    private fun callWithRole(origin: String, path: String, role: String) =
            Unirest.get(origin + path).queryString("role", role).asString().body

}
