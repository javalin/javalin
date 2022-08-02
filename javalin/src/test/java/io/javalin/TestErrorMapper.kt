/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpResponseException
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestErrorMapper {

    @Test
    fun `error-mapper works for 404`() = TestUtil.test { app, http ->
        app.error(NOT_FOUND) { it.result("Custom 404 page") }
        assertThat(http.getBody("/unmapped")).isEqualTo("Custom 404 page")
    }


    @Test
    fun `error-mapper works for 500`() = TestUtil.test { app, http ->
        app.get("/exception") { throw RuntimeException() }
            .error(INTERNAL_SERVER_ERROR) { it.result("Custom 500 page") }
        assertThat(http.getBody("/exception")).isEqualTo("Custom 500 page")
    }

    @Test
    fun `error-mapper works for custom code 555`() = TestUtil.test { app, http ->
        app.get("/exception") { throw HttpResponseException(555, "Error 555") }
            .error(555) { it.result("Custom 555 page") }
        assertThat(http.getBody("/exception")).isEqualTo("Custom 555 page")
    }

    @Test
    fun `error-mapper runs after exception-mapper`() = TestUtil.test { app, http ->
        app.get("/exception") { throw RuntimeException() }
            .exception(Exception::class.java) { _, ctx -> ctx.status(INTERNAL_SERVER_ERROR).result("Exception handled!") }
            .error(INTERNAL_SERVER_ERROR) { it.result("Custom 500 page") }
        assertThat(http.getBody("/exception")).isEqualTo("Custom 500 page")
    }

    @Test
    fun `error-mapper can throw exceptions`() = TestUtil.test { app, http ->
        app.get("/exception") { throw RuntimeException() }
            .exception(Exception::class.java) { _, ctx -> ctx.status(INTERNAL_SERVER_ERROR).result("Exception handled!") }
            .error(INTERNAL_SERVER_ERROR) { ctx ->
                ctx.result("Custom 500 page")
                throw RuntimeException()
            }
        assertThat(http.getBody("/exception")).isEqualTo("Exception handled!")
    }

    @Test
    fun `error-mapper with content-type respects content-type`() = TestUtil.test { app, http ->
        app.get("/html") { it.status(INTERNAL_SERVER_ERROR).result("Error!") }
        app.error(INTERNAL_SERVER_ERROR, "html") { it.result("HTML error page") }
        assertThat(http.htmlGet("/html").body).isEqualTo("HTML error page")
        assertThat(http.jsonGet("/html").body).isEqualTo("Error!")
    }

    @Test
    fun `error mapper is not overwritten`() = TestUtil.test { app, http ->
        app.error(INTERNAL_SERVER_ERROR, "html") { it.result("HTML error page") }
        app.error(INTERNAL_SERVER_ERROR, "json") { it.result("JSON error") }
        app.get("/html") { it.status(INTERNAL_SERVER_ERROR) }
        assertThat(http.htmlGet("/html").body).isEqualTo("HTML error page")
    }

}
