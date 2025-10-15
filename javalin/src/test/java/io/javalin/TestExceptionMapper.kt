/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.http.BadRequestResponse
import io.javalin.http.HttpResponseException
import io.javalin.http.HttpStatus
import io.javalin.http.HttpStatus.BAD_REQUEST
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.OK
import io.javalin.http.NotFoundResponse
import io.javalin.testing.TestUtil
import io.javalin.testing.TypedException
import io.javalin.testing.*
import io.javalin.testing.httpCode
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.io.EofException
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.TimeoutException
import kotlin.reflect.full.allSuperclasses

class TestExceptionMapper {

    @Test
    fun `unmapped exceptions are caught by default handler`() = TestUtil.test { app, http ->
        app.get("/unmapped-exception") { throw Exception() }
        assertThat(http.get("/unmapped-exception").httpCode()).isEqualTo(INTERNAL_SERVER_ERROR)
        assertThat(http.getBody("/unmapped-exception")).isEqualTo(INTERNAL_SERVER_ERROR.message)
    }

    @Test
    fun `exceptions in exception handlers use default handler`() = TestUtil.test { app, http ->
        app.exception(Exception::class.java) { e, ctx -> throw IllegalArgumentException("Exception in exception handler") }
        app.get("/") { throw NumberFormatException() }
        assertThat(http.get("/").httpCode()).isEqualTo(INTERNAL_SERVER_ERROR)
        assertThat(http.getBody("/")).isEqualTo(INTERNAL_SERVER_ERROR.message)
    }

    @Test
    fun `mapped exceptions are handled`() = TestUtil.test { app, http ->
        app.get("/mapped-exception") { throw Exception() }
            .exception(Exception::class.java) { _, ctx -> ctx.result("It's been handled.") }
        assertThat(http.get("/mapped-exception").httpCode()).isEqualTo(OK)
        assertThat(http.getBody("/mapped-exception")).isEqualTo("It's been handled.")
    }

    @Test
    fun `HttpResponseException subclass handler is used`() = TestUtil.test { app, http ->
        app.get("/mapped-http-response-exception") { throw NotFoundResponse() }
            .exception(NotFoundResponse::class.java) { _, ctx -> ctx.result("It's been handled.") }
        assertThat(http.get("/mapped-http-response-exception").httpCode()).isEqualTo(OK)
        assertThat(http.getBody("/mapped-exception")).isEqualTo("It's been handled.")
    }

    @Test
    fun `HttpResponseException handler is used for subclasses`() = TestUtil.test { app, http ->
        app.get("/mapped-http-response-exception") { throw NotFoundResponse() }
            .exception(HttpResponseException::class.java) { _, ctx -> ctx.result("It's been handled.") }
        assertThat(http.get("/mapped-http-response-exception").httpCode()).isEqualTo(OK)
        assertThat(http.getBody("/mapped-exception")).isEqualTo("It's been handled.")
    }

    @Test
    fun `type information of exception is not lost`() = TestUtil.test { app, http ->
        app.get("/typed-exception") { throw TypedException() }
            .exception(TypedException::class.java) { e, ctx -> ctx.result(e.proofOfType()) }
        assertThat(http.get("/typed-exception").httpCode()).isEqualTo(OK)
        assertThat(http.getBody("/typed-exception")).isEqualTo("I'm so typed")
    }

    @Test
    fun `most specific exception handler handles exception`() = TestUtil.test { app, http ->
        app.get("/exception-priority") { throw TypedException() }
            .exception(Exception::class.java) { _, ctx -> ctx.result("This shouldn't run") }
            .exception(TypedException::class.java) { _, ctx -> ctx.result("Typed!") }
        assertThat(http.get("/exception-priority").httpCode()).isEqualTo(OK)
        assertThat(http.getBody("/exception-priority")).isEqualTo("Typed!")
    }

    @Test
    fun `catch-all Exception mapper doesn't override 404`() = TestUtil.test { app, http ->
        app.exception(Exception::class.java) { _, ctx -> ctx.status(INTERNAL_SERVER_ERROR) }
        assertThat(http.get("/not-found").httpCode()).isEqualTo(NOT_FOUND)
    }

    @Test
    fun `catch-all Exception mapper doesn't override HttpResponseExceptions`() = TestUtil.test { app, http ->
        app.exception(Exception::class.java) { _, ctx -> ctx.status(INTERNAL_SERVER_ERROR) }
        app.get("/") { throw BadRequestResponse() }
        assertThat(http.get("/").httpCode()).isEqualTo(BAD_REQUEST)
    }

    @Test
    fun `exceptions can have a lot of superclasses`() = TestUtil.test { app, http ->
        app.exception(Exception::class.java) { e, ctx -> ctx.result(e.javaClass.kotlin.allSuperclasses.size.toString()) }
        app.get("/") { throw NumberFormatException() }
        assertThat(http.get("/").body).isEqualTo("6")
    }

    @Test
    fun `exception title can contain quotes`() = TestUtil.test { app, http ->
        app.get("/") { throw BadRequestResponse("""MY MESSAGE WITH "QUOTES"""") }
        assertThat(http.jsonGet("/").body).contains("""MY MESSAGE WITH \"QUOTES\"""")
    }

    @Test
    fun `exception title can contain newlines`() = TestUtil.test { app, http ->
        app.get("/") { throw BadRequestResponse("MY MESSAGE WITH \nNEWLINES\n") }
        assertThat(http.jsonGet("/").body).contains("""MY MESSAGE WITH \nNEWLINES\n""")
    }

    @Test
    fun `jetty eof exceptions are caught and handled as errors`() = TestUtil.test(Javalin.create {
        it.jetty.clientAbortStatus = 499
    }) { app, http ->
        app.get("/") { throw EofException() }
        assertThat(http.get("/").httpCode()).isEqualTo(HttpStatus.forStatus(499))
    }

    @Test
    fun `jetty timeout exceptions are caught and handled as errors`() = TestUtil.test(Javalin.create {
        it.jetty.timeoutStatus = 408
    }) { app, http ->
        app.get("/") { throw IOException("timeout", TimeoutException()) }
        assertThat(http.get("/").httpCode()).isEqualTo(HttpStatus.forStatus(408))
    }

    @Test
    fun `errors are handled by javaLangErrorHandler`() = TestUtil.test { app, http ->
        app.unsafeConfig().pvt.javaLangErrorHandler { res, throwable ->
            res.status = 200
            res.writer.write("Error: ${throwable.message}")
        }
        app.get("/") { throw OutOfMemoryError("sample") }
        assertThat(http.get("/").httpCode()).isEqualTo(OK)
        assertThat(http.getBody("/")).isEqualTo("Error: sample")
    }
}
