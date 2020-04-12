/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestErrorMapper {

    @Test
    fun `error-mapper works for 404`() = TestUtil.test { app, http ->
        app.error(404) { ctx -> ctx.result("Custom 404 page") }
        assertThat(http.getBody("/unmapped")).isEqualTo("Custom 404 page")
    }

    @Test
    fun `error-mapper works for 500`() = TestUtil.test { app, http ->
        app.get("/exception") { throw RuntimeException() }
                .error(500) { ctx -> ctx.result("Custom 500 page") }
        assertThat(http.getBody("/exception")).isEqualTo("Custom 500 page")
    }

    @Test
    fun `error-mapper runs after exception-mapper`() = TestUtil.test { app, http ->
        app.get("/exception") { throw RuntimeException() }
                .exception(Exception::class.java) { _, ctx -> ctx.status(500).result("Exception handled!") }
                .error(500) { ctx -> ctx.result("Custom 500 page") }
        assertThat(http.getBody("/exception")).isEqualTo("Custom 500 page")
    }

    @Test
    fun `error-mapper can throw exceptions`() = TestUtil.test { app, http ->
        app.get("/exception") { throw RuntimeException() }
                .exception(Exception::class.java) { _, ctx -> ctx.status(500).result("Exception handled!") }
                .error(500) { ctx ->
                    ctx.result("Custom 500 page")
                    throw RuntimeException()
                }
        assertThat(http.getBody("/exception")).isEqualTo("Exception handled!")
    }

    @Test
    fun `error-mapper with content-type respects content-type`() = TestUtil.test { app, http ->
        app.get("/html") { it.status(500).result("Error!") }.error(500, "html") { it.result("HTML error page") }
        assertThat(http.htmlGet("/html").body).isEqualTo("HTML error page")
        assertThat(http.jsonGet("/html").body).isEqualTo("Error!")
        app.get("/json") { it.status(500).result("Error!") }.error(500, "json") { it.result("JSON error") }
        assertThat(http.htmlGet("/json").body).isEqualTo("Error!")
        assertThat(http.jsonGet("/json").body).isEqualTo("JSON error")
    }

}
