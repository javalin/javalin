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
import io.javalin.http.Header.ACCESS_CONTROL_EXPOSE_HEADERS
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
    fun `throws for empty origins if reflectClientOrigin is false`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { Javalin.create { it.plugins.enableCors({}) } }
            .withMessageStartingWith("Origins cannot be empty if `reflectClientOrigin` is false.")
    }

    @Test
    fun `throws for non-empty if reflectClientOrigin is true`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java).isThrownBy {
            Javalin.create {
                it.plugins.enableCors {
                    it.reflectClientOrigin = true
                    it.allowHost("A", "B")
                }
            }
        }.withMessageStartingWith("Cannot set `allowedOrigins` if `reflectClientOrigin` is true")
    }

    @Test
    fun `can enable cors for specific origins`() = TestUtil.test(Javalin.create {
        it.plugins.enableCors { it.allowHost("origin-1", "referer-1") }
    }) { app, http ->
        app.get("/") { it.result("Hello") }
        assertThat(http.get("/").header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
        assertThat(http.get("/", mapOf(ORIGIN to "origin-1")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("origin-1")
        assertThat(http.get("/", mapOf(REFERER to "referer-1")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("referer-1")
    }

    @Test
    fun `can enable cors for star origins`() = TestUtil.test(Javalin.create {
        it.plugins.enableCors { it.anyHost() }
    }) { app, http ->
        app.get("/") { it.result("Hello") }
        assertThat(http.get("/", mapOf(ORIGIN to "A")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*")
        assertThat(http.get("/", mapOf(REFERER to "B")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*")
    }

    @Test
    fun `headers are not set when origin doesn't match`() = TestUtil.test(Javalin.create {
        it.plugins.enableCors { it.allowHost("origin-1.com") }
    }) { app, http ->
        app.get("/") { it.result("Hello") }
        assertThat(http.get("/", mapOf(ORIGIN to "origin-2.com")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
        assertThat(http.get("/", mapOf(ORIGIN to "origin-1.com.au")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
    }

    @Test
    fun `has allowsCredentials false by default`() = TestUtil.test(Javalin.create {
        it.plugins.enableCors { it.reflectClientOrigin = true }
    }) { app, http ->
        app.get("/") { it.result("Hello") }
        assertThat(http.get("/").header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
        assertThat(http.get("/", mapOf(ORIGIN to "some-origin")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("some-origin")
        assertThat(http.get("/", mapOf(ORIGIN to "some-origin")).header(ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEmpty() // cookies not allowed
        assertThat(http.get("/", mapOf(REFERER to "some-referer")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("some-referer")
    }

    @Test
    fun `can have allowsCredentials set true`() = TestUtil.test(Javalin.create {
        it.plugins.enableCors {
            it.reflectClientOrigin = true
            it.allowCredentials = true
        }
    }) { app, http ->
        app.get("/") { it.result("Hello") }
        assertThat(http.get("/").header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEmpty()
        assertThat(http.get("/", mapOf(ORIGIN to "some-origin")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("some-origin")
        assertThat(http.get("/", mapOf(ORIGIN to "some-origin")).header(ACCESS_CONTROL_ALLOW_CREDENTIALS)).isEqualTo("true") // cookies allowed
        assertThat(http.get("/", mapOf(REFERER to "some-referer")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("some-referer")
    }

    @Test
    fun `works for 404s`() = TestUtil.test(Javalin.create { it.plugins.enableCors { it.reflectClientOrigin = true } }) { app, http ->
        assertThat(http.get("/not-found", mapOf(ORIGIN to "some-origin")).header(ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("some-origin")
    }

    @Test
    fun `works with AccessManager`() = TestUtil.test(Javalin.create {
        it.plugins.enableCors { it.reflectClientOrigin = true }
        it.accessManager { _, ctx, _ -> ctx.status(UNAUTHORIZED).result(UNAUTHORIZED.message) }
    }) { app, http ->
        app.get("/", { it.result("Hello") }, TestAccessManager.MyRoles.ROLE_ONE)
        assertThat(http.get("/").body).isEqualTo(UNAUTHORIZED.message)
        val response = Unirest.options(http.origin)
            .header(ACCESS_CONTROL_REQUEST_HEADERS, "123")
            .header(ACCESS_CONTROL_REQUEST_METHOD, "TEST")
            .asString()
        assertThat(response.header(ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("123")
        assertThat(response.header(ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("TEST")
        assertThat(response.body).isBlank()
        assertThat(response.status).isEqualTo(200)
    }

    @Test
    fun `works with options endpoint mapping`() = TestUtil.test(Javalin.create {
        it.plugins.enableCors { it.reflectClientOrigin = true }
    }) { app, http ->
        app.options("/") { it.result("Hello") }
        val response = Unirest.options(http.origin)
            .header(ACCESS_CONTROL_REQUEST_HEADERS, "123")
            .header(ACCESS_CONTROL_REQUEST_METHOD, "TEST")
            .asString()
        assertThat(response.header(ACCESS_CONTROL_ALLOW_HEADERS)).isEqualTo("123")
        assertThat(response.header(ACCESS_CONTROL_ALLOW_METHODS)).isEqualTo("TEST")
        assertThat(response.body).isEqualTo("Hello")
    }

    @Test
    fun `allows exposing a single header`() = TestUtil.test(Javalin.create { cfg ->
        cfg.plugins.enableCors {
            it.reflectClientOrigin = true
            it.exposeHeader("x-test")
        }
    }) { app, http ->
        app.get("/") { it.result("Hello") }
        val response = Unirest.get(http.origin)
            .header(ORIGIN, "example.com")
            .asString()
        assertThat(response.header(ACCESS_CONTROL_EXPOSE_HEADERS)).isEqualTo("x-test")
        assertThat(response.body).isEqualTo("Hello")
    }

    @Test
    fun `allows exposing multiple headers`() = TestUtil.test(Javalin.create { cfg ->
        cfg.plugins.enableCors {
            it.reflectClientOrigin = true
            it.exposeHeader("x-test")
            it.exposeHeader("x-world")
        }
    }) { app, http ->
        app.get("/") { it.result("Hello") }
        val response = Unirest.get(http.origin)
            .header(ORIGIN, "example.com")
            .asString()
        assertThat(response.header(ACCESS_CONTROL_EXPOSE_HEADERS)).isEqualTo("x-test,x-world")
        assertThat(response.body).isEqualTo("Hello")
    }

    private fun HttpResponse<String>.header(name: String): String = this.headers.getFirst(name)

}
