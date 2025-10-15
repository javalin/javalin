/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.AcceptedResponse
import io.javalin.http.AlreadyReportedResponse
import io.javalin.http.BadGatewayResponse
import io.javalin.http.BadRequestResponse
import io.javalin.http.ConflictResponse
import io.javalin.http.ContentTooLargeResponse
import io.javalin.http.ContentType
import io.javalin.http.ContinueResponse
import io.javalin.http.CreatedResponse
import io.javalin.http.EarlyHintsResponse
import io.javalin.http.EnhanceYourCalmResponse
import io.javalin.http.ExpectationFailedResponse
import io.javalin.http.FailedDependencyResponse
import io.javalin.http.ForbiddenResponse
import io.javalin.http.FoundResponse
import io.javalin.http.GatewayTimeoutResponse
import io.javalin.http.GoneResponse
import io.javalin.http.Header
import io.javalin.http.HttpResponseException
import io.javalin.http.HttpStatus.BAD_REQUEST
import io.javalin.http.HttpStatus.FORBIDDEN
import io.javalin.http.HttpStatus.IM_A_TEAPOT
import io.javalin.http.HttpStatus.UNAUTHORIZED
import io.javalin.http.HttpVersionNotSupportedResponse
import io.javalin.http.ImATeapotResponse
import io.javalin.http.ImUsedResponse
import io.javalin.http.InsufficientStorageResponse
import io.javalin.http.InternalServerErrorResponse
import io.javalin.http.LengthRequiredResponse
import io.javalin.http.LockedResponse
import io.javalin.http.LoopDetectedResponse
import io.javalin.http.MethodNotAllowedResponse
import io.javalin.http.MisdirectedRequestResponse
import io.javalin.http.MovedPermanentlyResponse
import io.javalin.http.MultiStatusResponse
import io.javalin.http.MultipleChoicesResponse
import io.javalin.http.NetworkAuthenticationRequiredResponse
import io.javalin.http.NoContentResponse
import io.javalin.http.NonAuthoritativeInformationResponse
import io.javalin.http.NotAcceptableResponse
import io.javalin.http.NotFoundResponse
import io.javalin.http.NotImplementedResponse
import io.javalin.http.NotModifiedResponse
import io.javalin.http.OkResponse
import io.javalin.http.PartialContentResponse
import io.javalin.http.PaymentRequiredResponse
import io.javalin.http.PermanentRedirectResponse
import io.javalin.http.PreconditionFailedResponse
import io.javalin.http.PreconditionRequiredResponse
import io.javalin.http.ProcessingResponse
import io.javalin.http.ProxyAuthenticationRequiredResponse
import io.javalin.http.RangeNotSatisfiableResponse
import io.javalin.http.RedirectResponse
import io.javalin.http.RequestHeaderFieldsTooLargeResponse
import io.javalin.http.RequestTimeoutResponse
import io.javalin.http.ResetContentResponse
import io.javalin.http.SeeOtherResponse
import io.javalin.http.ServiceUnavailableResponse
import io.javalin.http.SwitchingProtocolsResponse
import io.javalin.http.TemporaryRedirectResponse
import io.javalin.http.TooEarlyResponse
import io.javalin.http.TooManyRequestsResponse
import io.javalin.http.UnauthorizedResponse
import io.javalin.http.UnavailableForLegalReasonsResponse
import io.javalin.http.UnprocessableContentResponse
import io.javalin.http.UnsupportedMediaTypeResponse
import io.javalin.http.UpgradeRequiredResponse
import io.javalin.http.UriTooLongResponse
import io.javalin.http.UseProxyResponse
import io.javalin.router.exception.HttpResponseExceptionMapper
import io.javalin.testing.TestUtil
import io.javalin.testing.*
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
    fun `response is formatted as text if client wants html or text`() = TestUtil.test { app, http ->
        app.post("/") { throw ForbiddenResponse("Forbidden", mapOf("a" to "A", "b" to "B")) }
        val response = http.post("/").header(Header.ACCEPT, ContentType.PLAIN).asString()
        assertThat(response.headers.getFirst(Header.CONTENT_TYPE)).isEqualTo(ContentType.PLAIN)
        assertThat(response.httpCode()).isEqualTo(FORBIDDEN)
        assertThat(response.body).isEqualTo("Forbidden\n\na:\nA\nb:\nB\n")
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
        app.get("/completed-future-route") { it.future { getExceptionallyCompletingFuture() } }
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
        app.get("/throwing-future-route") { it.future { getThrowingFuture() } }
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
        app.get("/completed-future-route") { it.future { getUnexpectedExceptionallyCompletingFuture() } }
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
        app.error(FORBIDDEN.code, "html") { it.result("Only mapped for HTML") }
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

    @Test
    fun `test all subclasses of HttpResponseException`() = TestUtil.test { app, http ->
        for (ex in allExceptions()) {
            val jsonResult = HttpResponseExceptionMapper.jsonResult(ex)
            assertThat(jsonResult).contains(ex.message)
            assertThat(jsonResult).contains(ex.status.toString())
        }
    }

    private fun allExceptions() = setOf(
        ContinueResponse(),
        SwitchingProtocolsResponse(),
        ProcessingResponse(),
        EarlyHintsResponse(),
        OkResponse(),
        CreatedResponse(),
        AcceptedResponse(),
        NonAuthoritativeInformationResponse(),
        NoContentResponse(),
        ResetContentResponse(),
        PartialContentResponse(),
        MultiStatusResponse(),
        AlreadyReportedResponse(),
        ImUsedResponse(),
        RedirectResponse(),
        MultipleChoicesResponse(),
        MovedPermanentlyResponse(),
        FoundResponse(),
        SeeOtherResponse(),
        NotModifiedResponse(),
        UseProxyResponse(),
        TemporaryRedirectResponse(),
        PermanentRedirectResponse(),
        BadRequestResponse(),
        UnauthorizedResponse(),
        PaymentRequiredResponse(),
        ForbiddenResponse(),
        NotFoundResponse(),
        MethodNotAllowedResponse(),
        NotAcceptableResponse(),
        ProxyAuthenticationRequiredResponse(),
        RequestTimeoutResponse(),
        ConflictResponse(),
        GoneResponse(),
        LengthRequiredResponse(),
        PreconditionFailedResponse(),
        ContentTooLargeResponse(),
        UriTooLongResponse(),
        UnsupportedMediaTypeResponse(),
        RangeNotSatisfiableResponse(),
        ExpectationFailedResponse(),
        ImATeapotResponse(),
        EnhanceYourCalmResponse(),
        MisdirectedRequestResponse(),
        UnprocessableContentResponse(),
        LockedResponse(),
        FailedDependencyResponse(),
        TooEarlyResponse(),
        UpgradeRequiredResponse(),
        PreconditionRequiredResponse(),
        TooManyRequestsResponse(),
        RequestHeaderFieldsTooLargeResponse(),
        UnavailableForLegalReasonsResponse(),
        InternalServerErrorResponse(),
        NotImplementedResponse(),
        BadGatewayResponse(),
        ServiceUnavailableResponse(),
        GatewayTimeoutResponse(),
        HttpVersionNotSupportedResponse(),
        InsufficientStorageResponse(),
        LoopDetectedResponse(),
        NetworkAuthenticationRequiredResponse()
    )

}
