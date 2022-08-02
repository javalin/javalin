/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.Header
import io.javalin.http.BadRequestResponse
import io.javalin.http.ContentType
import io.javalin.http.ForbiddenResponse
import io.javalin.http.HttpResponseException
import io.javalin.http.UnauthorizedResponse
import io.javalin.http.HttpStatus.BAD_REQUEST
import io.javalin.http.HttpStatus.FORBIDDEN
import io.javalin.http.HttpStatus.IM_A_TEAPOT
import io.javalin.http.HttpStatus.UNAUTHORIZED
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TestHttpResponseExceptions {

    @Test
    fun `default values work`() = TestUtil.test { app, http ->
        app.get("/") { throw BadRequestResponse() }
        assertThat(http.getBody("/")).isEqualTo(BAD_REQUEST.message)
        assertThat(http.get("/").httpCode()).isEqualTo(BAD_REQUEST)
    }

    @Test
    fun `custom message works`() = TestUtil.test { app, http ->
        app.get("/") { throw BadRequestResponse("Really bad request") }
        assertThat(http.getBody("/")).isEqualTo("Really bad request")
        assertThat(http.get("/").httpCode()).isEqualTo(BAD_REQUEST)
    }

    @Test
    fun `response is formatted as text if client wants text`() = TestUtil.test { app, http ->
        app.post("/") { throw ForbiddenResponse() }
        val response = http.post("/").header(Header.ACCEPT, ContentType.PLAIN).asString()
        assertThat(response.headers.getFirst(Header.CONTENT_TYPE)).isEqualTo(ContentType.PLAIN)
        assertThat(response.httpCode()).isEqualTo(FORBIDDEN)
        assertThat(response.body).isEqualTo("Forbidden")
    }

    @Test
    fun `response is formatted as json if client wants json`() = TestUtil.test { app, http ->
        app.post("/") { throw ForbiddenResponse("Off limits!") }
        val response = http.post("/").header(Header.ACCEPT, ContentType.JSON).asString()
        assertThat(response.headers.getFirst(Header.CONTENT_TYPE)).isEqualTo(ContentType.JSON)
        assertThat(response.httpCode()).isEqualTo(FORBIDDEN)
        assertThat(response.body).isEqualTo(
            """{
            |    "title": "Off limits!",
            |    "status": 403,
            |    "type": "https://javalin.io/documentation#forbiddenresponse",
            |    "details": {}
            |}""".trimMargin()
        )
    }

    class CustomResponse : HttpResponseException(IM_A_TEAPOT, "", mapOf())

    @Test
    fun `custom response has default type`() = TestUtil.test { app, http ->
        app.post("/") { throw CustomResponse() }
        val response = http.post("/").header(Header.ACCEPT, ContentType.JSON).asString()
        assertThat(response.httpCode()).isEqualTo(IM_A_TEAPOT)
        assertThat(response.body).isEqualTo(
            """{
                |    "title": "",
                |    "status": 418,
                |    "type": "https://javalin.io/documentation#error-responses",
                |    "details": {}
                |}""".trimMargin()
        )
    }

    class CustomIntResponse : HttpResponseException(555, "Error 555", mapOf())

    @Test
    fun `custom int response has default type`() = TestUtil.test { app, http ->
        app.post("/") { throw CustomIntResponse() }
        val response = http.post("/").header(Header.ACCEPT, ContentType.JSON).asString()
        assertThat(response.status).isEqualTo(555)
        assertThat(response.body).isEqualTo(
            """{
                |    "title": "Error 555",
                |    "status": 555,
                |    "type": "https://javalin.io/documentation#error-responses",
                |    "details": {}
                |}""".trimMargin()
        )
    }

    @Test
    fun `throwing HttpResponseExceptions in before-handler works`() = TestUtil.test { app, http ->
        app.before("/admin/*") { throw UnauthorizedResponse() }
        app.get("/admin/protected") { it.result("Protected resource") }
        assertThat(http.get("/admin/protected").httpCode()).isEqualTo(UNAUTHORIZED)
        assertThat(http.getBody("/admin/protected")).isNotEqualTo("Protected resource")
    }

    @Test
    fun `throwing HttpResponseExceptions in endpoint-handler works`() = TestUtil.test { app, http ->
        app.get("/some-route") { throw UnauthorizedResponse("Stop!") }
        assertThat(http.get("/some-route").httpCode()).isEqualTo(UNAUTHORIZED)
        assertThat(http.getBody("/some-route")).isEqualTo("Stop!")
    }

    @Test
    fun `after-handlers execute after HttpResponseExceptions`() = TestUtil.test { app, http ->
        app.get("/some-route") { throw UnauthorizedResponse("Stop!") }
        app.after { it.status(IM_A_TEAPOT) }
        assertThat(http.get("/some-route").httpCode()).isEqualTo(IM_A_TEAPOT)
        assertThat(http.getBody("/some-route")).isEqualTo("Stop!")
    }

    @Test
    fun `completing exceptionally with HttpResponseExceptions in future works`() = TestUtil.test { app, http ->
        fun getExceptionallyCompletingFuture(): CompletableFuture<String> {
            val future = CompletableFuture<String>()
            Executors.newSingleThreadScheduledExecutor().schedule({
                future.completeExceptionally(UnauthorizedResponse())
            }, 0, TimeUnit.MILLISECONDS)
            return future
        }
        app.get("/completed-future-route") { it.future(getExceptionallyCompletingFuture()) }
        assertThat(http.get("/completed-future-route").body).isEqualTo("Unauthorized")
        assertThat(http.get("/completed-future-route").httpCode()).isEqualTo(UNAUTHORIZED)
    }

    @Test
    fun `throwing HttpResponseExceptions in future works`() = TestUtil.test { app, http ->
        fun getThrowingFuture() = CompletableFuture.supplyAsync {
            if (Math.random() < 2) { // it's true!
                throw UnauthorizedResponse()
            }
            "Result"
        }
        app.get("/throwing-future-route") { it.future(getThrowingFuture()) }
        assertThat(http.get("/throwing-future-route").body).isEqualTo("Unauthorized")
        assertThat(http.get("/throwing-future-route").httpCode()).isEqualTo(UNAUTHORIZED)
    }

    @Test
    fun `completing exceptionally with unexpected exceptions in future works`() = TestUtil.test { app, http ->
        fun getUnexpectedExceptionallyCompletingFuture(): CompletableFuture<String> {
            val future = CompletableFuture<String>()
            Executors.newSingleThreadScheduledExecutor().schedule({
                future.completeExceptionally(IllegalStateException("Unexpected message"))
            }, 0, TimeUnit.MILLISECONDS)
            return future
        }
        app.get("/completed-future-route") { it.future(getUnexpectedExceptionallyCompletingFuture()) }
        app.exception(IllegalStateException::class.java) { exception, ctx -> ctx.result(exception.message!!) }
        assertThat(http.get("/completed-future-route").body).isEqualTo("Unexpected message")
    }

    @Test
    fun `default content type affects http response errors`() = TestUtil.test(Javalin.create { it.http.defaultContentType = ContentType.JSON }) { app, http ->
        app.get("/content-type") { throw ForbiddenResponse() }
        val response = http.get("/content-type")
        assertThat(response.httpCode()).isEqualTo(FORBIDDEN)
        assertThat(response.headers.getFirst(Header.CONTENT_TYPE)).isEqualTo(ContentType.JSON)
        assertThat(response.body).isEqualTo(
            """{
            |    "title": "Forbidden",
            |    "status": 403,
            |    "type": "https://javalin.io/documentation#forbiddenresponse",
            |    "details": {}
            |}""".trimMargin()
        )
    }

    @Test
    fun `default exceptions work well with custom content-typed errors`() = TestUtil.test { app, http ->
        app.get("/") { throw ForbiddenResponse("Off limits!") }
        app.error(FORBIDDEN, "html") { it.result("Only mapped for HTML") }
        assertThat(http.jsonGet("/").body).isEqualTo(
            """{
            |    "title": "Off limits!",
            |    "status": 403,
            |    "type": "https://javalin.io/documentation#forbiddenresponse",
            |    "details": {}
            |}""".trimMargin()
        )
        assertThat(http.htmlGet("/").body).isEqualTo("Only mapped for HTML")
    }

    @Test
    fun `can override HttpResponseExceptions`() = TestUtil.test { app, http ->
        val randomNumberString = (Math.random() * 10000).toString()
        app.get("/") { throw BadRequestResponse() }
        app.exception(BadRequestResponse::class.java) { _, ctx -> ctx.result(randomNumberString) }
        assertThat(http.getBody("/")).isEqualTo(randomNumberString)
    }

    @Test
    fun `details are displayed as a map in json`() = TestUtil.test { app, http ->
        app.get("/") { throw ForbiddenResponse("Off limits!", mapOf("a" to "A", "b" to "B")) }
        assertThat(http.jsonGet("/").body).isEqualTo(
            """{
            |    "title": "Off limits!",
            |    "status": 403,
            |    "type": "https://javalin.io/documentation#forbiddenresponse",
            |    "details": {"a":"A","b":"B"}
            |}""".trimMargin()
        )
    }

}
