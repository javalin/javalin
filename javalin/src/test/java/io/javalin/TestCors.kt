/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.core.util.Header.ACCESS_CONTROL_ALLOW_CREDENTIALS
import io.javalin.core.util.Header.ACCESS_CONTROL_ALLOW_HEADERS
import io.javalin.core.util.Header.ACCESS_CONTROL_ALLOW_METHODS
import io.javalin.core.util.Header.ACCESS_CONTROL_ALLOW_ORIGIN
import io.javalin.core.util.Header.ACCESS_CONTROL_REQUEST_HEADERS
import io.javalin.core.util.Header.ACCESS_CONTROL_REQUEST_METHOD
import io.javalin.core.util.Header.ORIGIN
import io.javalin.core.util.Header.REFERER
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test

class TestCors {

    @Test
    fun `enableCorsForOrigin throws for empty varargs`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
                .isThrownBy { Javalin.create { it.enableCorsForOrigin() } }
                .withMessageStartingWith("Origins cannot be empty.")
    }

    @Test
    fun `enableCorsForOrigin enables cors for specific origins`() {
        val javalin = Javalin.create { it.enableCorsForOrigin("origin-1", "referer-1") }
        TestUtil.test(javalin) { app, http ->
            app.get("/") { ctx -> ctx.result("Hello") }
            assertThat(Unirest.get(http.origin).asString().headers[ACCESS_CONTROL_ALLOW_ORIGIN]).isNull()
            assertThat(Unirest.get(http.origin).header(ORIGIN, "origin-1").asString().headers[ACCESS_CONTROL_ALLOW_ORIGIN]!![0]).isEqualTo("origin-1")
            assertThat(Unirest.get(http.origin).header(REFERER, "referer-1").asString().headers[ACCESS_CONTROL_ALLOW_ORIGIN]!![0]).isEqualTo("referer-1")
        }
    }

    @Test
    fun `rejectCorsForInvalidOrigin reject where origin doesn't match`() {
        val javalin = Javalin.create { it.enableCorsForOrigin("origin-1.com") }
        TestUtil.test(javalin) { app, http ->
            app.get("/") { ctx -> ctx.result("Hello") }
            assertThat(Unirest.get(http.origin).header(ORIGIN, "origin-2.com").asString().headers[ACCESS_CONTROL_ALLOW_ORIGIN]).isNull()
            assertThat(Unirest.get(http.origin).header(ORIGIN, "origin-1.com.au").asString().headers[ACCESS_CONTROL_ALLOW_ORIGIN]).isNull()
        }
    }

    @Test
    fun `enableCorsForAllOrigins enables cors for all origins`() {
        val javalin = Javalin.create { it.enableCorsForAllOrigins() }
        TestUtil.test(javalin) { app, http ->
            app.get("/") { ctx -> ctx.result("Hello") }
            assertThat(Unirest.get(http.origin).header("Origin", "some-origin").asString().headers[ACCESS_CONTROL_ALLOW_ORIGIN]!![0]).isEqualTo("some-origin")
            assertThat(Unirest.get(http.origin).header("Origin", "some-origin").asString().headers[ACCESS_CONTROL_ALLOW_CREDENTIALS]!![0]).isEqualTo("true") // cookies allowed
            assertThat(Unirest.get(http.origin).header("Referer", "some-referer").asString().headers[ACCESS_CONTROL_ALLOW_ORIGIN]!![0]).isEqualTo("some-referer")
            assertThat(Unirest.get(http.origin).asString().headers[ACCESS_CONTROL_ALLOW_ORIGIN]).isNull()
        }
    }


    @Test
    fun `enableCorsForAllOrigins enables cors for all origins with AccessManager`() {
        val accessManagedCorsApp = Javalin.create {
            it.enableCorsForAllOrigins()
            it.accessManager { _, ctx, _ ->
                ctx.status(401).result("Unauthorized")
            }
        }
        TestUtil.test(accessManagedCorsApp) { app, http ->
            app.get("/", { ctx -> ctx.result("Hello") }, setOf(TestAccessManager.MyRoles.ROLE_ONE))
            assertThat(http.get("/").body).isEqualTo("Unauthorized")
            val response = Unirest.options(http.origin)
                    .header(ACCESS_CONTROL_REQUEST_HEADERS, "123")
                    .header(ACCESS_CONTROL_REQUEST_METHOD, "TEST")
                    .asString()
            assertThat(response.headers[ACCESS_CONTROL_ALLOW_HEADERS]!![0]).isEqualTo("123")
            assertThat(response.headers[ACCESS_CONTROL_ALLOW_METHODS]!![0]).isEqualTo("TEST")
            assertThat(response.body).isBlank()
        }
    }


    @Test
    fun `enableCorsForAllOrigins allows options endpoint mapping`() {
        val javalin = Javalin.create { it.enableCorsForAllOrigins() }
        TestUtil.test(javalin) { app, http ->
            app.options("/") { ctx -> ctx.result("Hello") }
            val response = Unirest.options(http.origin)
                    .header(ACCESS_CONTROL_REQUEST_HEADERS, "123")
                    .header(ACCESS_CONTROL_REQUEST_METHOD, "TEST")
                    .asString()
            assertThat(response.headers[ACCESS_CONTROL_ALLOW_HEADERS]!![0]).isEqualTo("123")
            assertThat(response.headers[ACCESS_CONTROL_ALLOW_METHODS]!![0]).isEqualTo("TEST")
            assertThat(response.body).isEqualTo("Hello")
        }
    }

}
