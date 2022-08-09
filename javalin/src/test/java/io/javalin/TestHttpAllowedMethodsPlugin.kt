/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.Header
import io.javalin.http.HttpStatus.CREATED
import io.javalin.http.HttpStatus.NOT_MODIFIED
import io.javalin.http.HttpStatus.OK
import io.javalin.testing.TestUtil
import kong.unirest.Unirest
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TestHttpAllowedMethodsPlugin {
    @Test
    fun `enableHttpOptionsForRoutes allows possible methods on routes`() {
        val javalin = Javalin.create { it.plugins.enableHttpAllowedMethodsOnRoutes() }
        javalin.get("/") { it.result("Hello") }
        javalin.delete("/") { it.status(OK) }

        javalin.get("/users") { it.result("Users") }
        javalin.post("/users") { it.status(CREATED) }
        javalin.patch("/users") { it.status(NOT_MODIFIED) }

        TestUtil.test(javalin) { app, http ->
            val response = Unirest.options(http.origin)
                .header(Header.ACCESS_CONTROL_REQUEST_HEADERS, "123")
                .header(Header.ACCESS_CONTROL_REQUEST_METHOD, "TEST")
                .asString()

            Assertions.assertThat(response.headers[Header.ACCESS_CONTROL_ALLOW_METHODS]!![0])
                .isEqualTo("GET,DELETE,OPTIONS")

            val usersResponse = Unirest.options(http.origin + "/users").asString()
            Assertions.assertThat(usersResponse.headers[Header.ACCESS_CONTROL_ALLOW_METHODS]!![0])
                .isEqualTo("GET,POST,PATCH,OPTIONS")
        }
    }
}
