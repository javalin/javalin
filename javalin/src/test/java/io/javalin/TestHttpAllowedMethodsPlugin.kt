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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestHttpAllowedMethodsPlugin {

    @Test
    fun `enableHttpOptionsForRoutes allows possible methods on routes`() {
        val javalin = Javalin.create { it.registerPlugin(HttpAllowedMethodsPlugin()) }
        javalin.get("/") { it.result("Hello") }
        javalin.delete("/") { it.status(OK) }
        javalin.get("/users") { it.result("Users") }
        javalin.post("/users") { it.status(CREATED) }
        javalin.patch("/users") { it.status(NOT_MODIFIED) }

        TestUtil.test(javalin) { app, http ->
            val response = http.call("OPTIONS", "/", mapOf(
                ACCESS_CONTROL_REQUEST_HEADERS to "123",
                ACCESS_CONTROL_REQUEST_METHOD to "TEST"
            ))

            assertThat(response.headers.getFirst(ACCESS_CONTROL_ALLOW_METHODS))
                .isEqualTo("GET,DELETE,OPTIONS")

            val usersResponse = http.call("OPTIONS", "/users")

            assertThat(usersResponse.headers.getFirst(ACCESS_CONTROL_ALLOW_METHODS))
                .isEqualTo("GET,POST,PATCH,OPTIONS")
        }
    }

}
