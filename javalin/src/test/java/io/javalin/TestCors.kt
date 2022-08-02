/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_CREDENTIALS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_METHODS
import io.javalin.http.Header.ACCESS_CONTROL_ALLOW_ORIGIN
import io.javalin.http.Header.ACCESS_CONTROL_REQUEST_HEADERS
import io.javalin.http.Header.ACCESS_CONTROL_REQUEST_METHOD
import io.javalin.http.Header.ORIGIN
import io.javalin.http.Header.REFERER
import io.javalin.http.HttpStatus.UNAUTHORIZED
import io.javalin.testing.TestUtil
import kong.unirest.HttpResponse
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

class TestCors {

    @Test
    fun `enableCorsForOrigin throws for empty origins if allowAll is false`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { Javalin.create { it.plugins.enableCors({}) } }
            .withMessageStartingWith("Origins cannot be empty if `allowAllOrigins` is false.")
    }

    @Test
    fun `enableCorsForOrigin throws for non-empty if allowAll is true`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            Javalin.create {
                it.plugins.enableCors {
                    it.allowAllOrigins = true
                    it.allowedOrigins = setOf("A", "B")
                }
            }
        }.withMessageStartingWith("Cannot set `allowedOrigins` if `allowAllOrigins` is true")
    }

    @Test
    fun `enableCorsForOrigin enables cors for specific origins`() {
        val javalin = Javalin.create {
            it.plugins.enableCors { it.allowedOrigins = setOf("origin-1", "referer-1") }
        }
        TestUtil.test(javalin) { app, http ->
            app.get("/") { it.result("Hello") }
            assertThat(http.get("/").header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
            assertThat(http.get("/", mapOf(ORIGIN to "origin-1")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("origin-1")
            assertThat(http.get("/", mapOf(REFERER to "referer-1")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("referer-1")
        }
    }

    @Test
    fun `rejectCorsForInvalidOrigin reject where origin doesn't match`() {
        val javalin = Javalin.create {
            it.plugins.enableCors { it.allowedOrigins = setOf("origin-1.com") }
        }
        TestUtil.test(javalin) { app, http ->
            app.get("/") { it.result("Hello") }
            assertThat(http.get("/", mapOf(ORIGIN to "origin-2.com")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
            assertThat(http.get("/", mapOf(ORIGIN to "origin-1.com.au")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
        }
    }

    @Test
    fun `enableCorsForAllOrigins has allowsCredentials false by default`() {
        val javalin = Javalin.create { it.plugins.enableCors { it.allowAllOrigins = true } }
        TestUtil.test(javalin) { app, http ->
            app.get("/") { it.result("Hello") }
            assertThat(http.get("/").header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
            assertThat(http.get("/", mapOf(ORIGIN to "some-origin")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("some-origin")
            assertThat(http.get("/", mapOf(ORIGIN to "some-origin")).header(ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEmpty() // cookies not allowed
            assertThat(http.get("/", mapOf(REFERER to "some-referer")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("some-referer")
        }
    }

    @Test
    fun `enableCorsForAllOrigins can have allowsCredentials set true`() {
        val javalin = Javalin.create {
            it.plugins.enableCors {
                it.allowAllOrigins = true
                it.allowCredentials = true
            }
        }
        TestUtil.test(javalin) { app, http ->
            app.get("/") { it.result("Hello") }
            assertThat(http.get("/").header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
            assertThat(http.get("/", mapOf(ORIGIN to "some-origin")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("some-origin")
            assertThat(http.get("/", mapOf(ORIGIN to "some-origin")).header(ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true") // cookies allowed
            assertThat(http.get("/", mapOf(REFERER to "some-referer")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("some-referer")
        }
    }

    @Test
    fun `enableCorsForAllOrigins works for 404s`() = TestUtil.test(Javalin.create { it.plugins.enableCors { it.allowAllOrigins = true } }) { app, http ->
        assertThat(http.get("/not-found", mapOf(ORIGIN to "some-origin")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("some-origin")
    }

    @Test
    fun `enableCorsForAllOrigins enables cors for all origins with AccessManager`() {
        val accessManagedCorsApp = Javalin.create {
            it.plugins.enableCors { it.allowAllOrigins = true }
            it.core.accessManager { _, ctx, _ ->
                ctx.status(UNAUTHORIZED).result(UNAUTHORIZED.message)
            }
        }
        TestUtil.test(accessManagedCorsApp) { app, http ->
            app.get("/", { it.result("Hello") }, TestAccessManager.MyRoles.ROLE_ONE)
            assertThat(http.get("/").body).isEqualTo(UNAUTHORIZED.message)
            val response = Unirest.options(http.origin)
                .header(ACCESS_CONTROL_REQUEST_HEADERS, "123")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "TEST")
                .asString()
            assertThat(response.header(ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("123")
            assertThat(response.header(ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("TEST")
            assertThat(response.body).isBlank()
        }
    }

    @Test
    fun `enableCorsForAllOrigins allows options endpoint mapping`() {
        val javalin = Javalin.create { it.plugins.enableCors { it.allowAllOrigins = true } }
        TestUtil.test(javalin) { app, http ->
            app.options("/") { it.result("Hello") }
            val response = Unirest.options(http.origin)
                .header(ACCESS_CONTROL_REQUEST_HEADERS, "123")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "TEST")
                .asString()
            assertThat(response.header(ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("123")
            assertThat(response.header(ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("TEST")
            assertThat(response.body).isEqualTo("Hello")
        }
    }

    private fun HttpResponse<String>.header(name: String): String = this.headers.getFirst(name)

}
