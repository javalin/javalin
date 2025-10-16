/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_METHODS
import io.javalin.http.Header.ACCESS_CONTROL_REQUEST_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_REQUEST_METHOD
import io.javalin.http.HttpStatus.CREATED
import io.javalin.http.HttpStatus.NOT_MODIFIED
import io.javalin.http.HttpStatus.OK
import io.javalin.plugin.bundled.HttpAllowedMethodsPlugin
import io.javalin.testing.TestUtil

import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestHttpAllowedMethodsPlugin {

    @Test
    fun `enableHttpOptionsForRoutes allows possible methods on routes`() {
        val javalin = Javalin.create {
            it.registerPlugin(HttpAllowedMethodsPlugin())
            it.routes.get("/") { it.result("Hello") }
            it.routes.delete("/") { it.status(OK) }
            it.routes.get("/users") { it.result("Users") }
            it.routes.post("/users") { it.status(CREATED) }
            it.routes.patch("/users") { it.status(NOT_MODIFIED) }
        }

        TestUtil.test(javalin) { app, http ->
            val response = Unirest.options(http.origin)
                .header(ACCESS_CONTROL_REQUEST_HEADERS, "123")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "TEST")
                .asString()

            assertThat(response.headers[ACCESS_CONTROL_ALLOW_METHODS].first())
                .isEqualTo("GET,DELETE,OPTIONS")

            val usersResponse = Unirest.options(http.origin + "/users")
                .asString()

            assertThat(usersResponse.headers[ACCESS_CONTROL_ALLOW_METHODS].first())
                .isEqualTo("GET,POST,PATCH,OPTIONS")
        }
    }

}
