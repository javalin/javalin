/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestErrorMapper {

    @Test
    fun `error-mapper works for 404`() = TestUtil.test { app, http ->
        app.error(404) { ctx -> ctx.result("Custom 404 page") }
        assertThat(http.getBody("/unmapped"), `is`("Custom 404 page"))
    }

    @Test
    fun `error-mapper works for 500`() = TestUtil.test { app, http ->
        app.get("/exception") { ctx -> throw RuntimeException() }
                .error(500) { ctx -> ctx.result("Custom 500 page") }
        assertThat(http.getBody("/exception"), `is`("Custom 500 page"))
    }

    @Test
    fun `error-mapper runs after exception-mapper`() = TestUtil.test { app, http ->
        app.get("/exception") { ctx -> throw RuntimeException() }
                .exception(Exception::class.java) { e, ctx -> ctx.status(500).result("Exception handled!") }
                .error(500) { ctx -> ctx.result("Custom 500 page") }
        assertThat(http.getBody("/exception"), `is`("Custom 500 page"))
    }

    @Test
    fun `error-mapper can throw exceptions`() = TestUtil.test { app, http ->
        app.get("/exception") { ctx -> throw RuntimeException() }
                .exception(Exception::class.java) { e, ctx -> ctx.status(500).result("Exception handled!") }
                .error(500) { ctx ->
                    ctx.result("Custom 500 page")
                    throw RuntimeException()
                }
        assertThat(http.getBody("/exception"), `is`("Exception handled!"))
    }

}
