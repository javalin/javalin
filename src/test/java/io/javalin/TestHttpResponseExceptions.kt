/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.Header
import io.javalin.util.TestUtil
import org.eclipse.jetty.http.HttpStatus
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class TestHttpResponseExceptions {

    @Test
    fun `default values work`() = TestUtil.test { app, http ->
        app.get("/") { throw BadRequestResponse() }
        assertThat(http.getBody("/"), `is`("Bad request"))
        assertThat(http.get("/").status, `is`(HttpStatus.BAD_REQUEST_400))
    }

    @Test
    fun `custom message works`() = TestUtil.test { app, http ->
        app.get("/") { throw BadRequestResponse("Really bad request") }
        assertThat(http.getBody("/"), `is`("Really bad request"))
        assertThat(http.get("/").status, `is`(HttpStatus.BAD_REQUEST_400))
    }

    @Test
    fun `response is formatted as text if client wants text`() = TestUtil.test { app, http ->
        app.post("/") { throw ForbiddenResponse() }
        val response = http.post("/").header(Header.ACCEPT, "text/plain").asString()
        assertThat(response.headers.getFirst(Header.CONTENT_TYPE), `is`("text/plain"))
        assertThat(response.status, `is`(HttpStatus.FORBIDDEN_403))
        assertThat(response.body, `is`("Forbidden"))
    }

    @Test
    fun `response is formatted as json if client wants json`() = TestUtil.test { app, http ->
        app.post("/") { throw ForbiddenResponse("Off limits!") }
        val response = http.post("/").header(Header.ACCEPT, "application/json").asString()
        assertThat(response.headers.getFirst(Header.CONTENT_TYPE), `is`("application/json"))
        assertThat(response.status, `is`(HttpStatus.FORBIDDEN_403))
        assertThat(response.body, `is`("""{
                |    "title": "Off limits!",
                |    "status": 403,
                |    "type": "https://javalin.io/documentation#forbiddenresponse",
                |    "details": []
                |}""".trimMargin()
        ))
    }

    class CustomResponse : HttpResponseException(418, "")

    @Test
    fun `custom response has default type`() = TestUtil.test { app, http ->
        app.post("/") { throw CustomResponse() }
        val response = http.post("/").header(Header.ACCEPT, "application/json").asString()
        assertThat(response.status, `is`(418))
        assertThat(response.body, `is`("""{
                |    "title": "",
                |    "status": 418,
                |    "type": "https://javalin.io/documentation#error-responses",
                |    "details": []
                |}""".trimMargin()
        ))
    }

    @Test
    fun `throwing HttpResponseExceptions in before-handler works`() = TestUtil.test { app, http ->
        app.before("/admin/*") { throw UnauthorizedResponse() }
        app.get("/admin/protected") { ctx -> ctx.result("Protected resource") }
        assertThat(http.get("/admin/protected").status, `is`(401))
        assertThat(http.getBody("/admin/protected"), not("Protected resource"))
    }

    @Test
    fun `throwing HttpResponseExceptions in endpoint-handler works`() = TestUtil.test { app, http ->
        app.get("/some-route") { throw UnauthorizedResponse("Stop!") }
        assertThat(http.get("/some-route").status, `is`(401))
        assertThat(http.getBody("/some-route"), `is`("Stop!"))
    }

    @Test
    fun `after-handlers execute after HttpResponseExceptions`() = TestUtil.test { app, http ->
        app.get("/some-route") { throw UnauthorizedResponse("Stop!") }
        app.after { ctx -> ctx.status(418) }
        assertThat(http.get("/some-route").status, `is`(418))
        assertThat(http.getBody("/some-route"), `is`("Stop!"))
    }

    @Test
    fun `completing exceptionally with HttpResponseExceptions in future works`() = TestUtil.test { app, http ->
        app.get("/completed-future-route") { ctx -> ctx.result(getExceptionallyCompletingFuture()) }
        assertThat(http.get("/completed-future-route").body, `is`("Unauthorized"))
        assertThat(http.get("/completed-future-route").status, `is`(401))
    }

    private fun getExceptionallyCompletingFuture(): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        Executors.newSingleThreadScheduledExecutor().schedule({
            future.completeExceptionally(UnauthorizedResponse())
        }, 10, TimeUnit.MILLISECONDS)
        return future
    }

    @Test
    fun `throwing HttpResponseExceptions in future works`() = TestUtil.test { app, http ->
        app.get("/throwing-future-route") { ctx -> ctx.result(getThrowingFuture()) }
        assertThat(http.get("/throwing-future-route").body, `is`("Unauthorized"))
        assertThat(http.get("/throwing-future-route").status, `is`(401))
    }

    private fun getThrowingFuture() = CompletableFuture.supplyAsync {
        if (true) {
            throw UnauthorizedResponse()
        }
        "Result"
    }

    @Test
    fun `completing exceptionally with unexpected exceptions in future works`() = TestUtil.test { app, http ->
        app.get("/completed-future-route") { ctx -> ctx.result(getUnexpectedExceptionallyCompletingFuture()) }
        app.exception(IllegalStateException::class.java) { exception, ctx -> ctx.result(exception.message!!) }
        assertThat(http.get("/completed-future-route").body, `is`("Unexpected message"))
    }

    private fun getUnexpectedExceptionallyCompletingFuture(): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        Executors.newSingleThreadScheduledExecutor().schedule({
            future.completeExceptionally(IllegalStateException("Unexpected message"))
        }, 10, TimeUnit.MILLISECONDS)
        return future
    }

    @Test
    fun `throwing unexpected exception in future works`() = TestUtil.test { app, http ->
        app.get("/throwing-future-route") { ctx -> ctx.result(getUnexpectedThrowingFuture()) }
        app.exception(CompletionException::class.java) { exception, ctx -> ctx.result(exception.cause?.message!!) }
        assertThat(http.get("/throwing-future-route").body, `is`("Unexpected message"))
    }

    private fun getUnexpectedThrowingFuture() = CompletableFuture.supplyAsync {
        if (true) {
            throw IllegalStateException("Unexpected message")
        }
        "Result"
    }

}
