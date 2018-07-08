/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.HttpMethod
import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestFilters {

    @Test
    fun `filters run before root handler`() = TestUtil.test { app, http ->
        app.before { ctx -> ctx.header("X-BEFOREFILTER", "Before-filter ran") }
        app.after { ctx -> ctx.header("X-AFTERFILTER", "After-filter ran") }
        app.get("/", TestUtil.okHandler)
        assertThat(http.get("/").headers.getFirst("X-BEFOREFILTER"), `is`("Before-filter ran"))
        assertThat(http.get("/").headers.getFirst("X-AFTERFILTER"), `is`("After-filter ran"))
    }

    @Test
    fun `app returns 404 for GET if only before-handler present`() = TestUtil.test { app, http ->
        app.before(TestUtil.okHandler)
        val response = http.call(HttpMethod.GET, "/hello")
        assertThat(response.status, `is`(404))
        assertThat(response.body, `is`("Not found"))
    }

    @Test
    fun `app returns 404 for POST if only before-handler present`() = TestUtil.test { app, http ->
        app.before(TestUtil.okHandler)
        val response = http.call(HttpMethod.POST, "/hello")
        assertThat(response.status, `is`(404))
        assertThat(response.body, `is`("Not found"))
    }

    @Test
    fun `before-handler can set header`() = TestUtil.test { app, http ->
        app.before { ctx -> ctx.header("X-FILTER", "Before-filter ran") }
        app.get("/mapped", TestUtil.okHandler)
        assertThat(http.get("/maped").headers.getFirst("X-FILTER"), `is`("Before-filter ran"))
    }

    @Test
    fun `before- and after-handlers can set a bunch of headers`() = TestUtil.test { app, http ->
        app.before { ctx -> ctx.header("X-BEFORE-1", "Before-filter 1 ran") }
        app.before { ctx -> ctx.header("X-BEFORE-2", "Before-filter 2 ran") }
        app.after { ctx -> ctx.header("X-AFTER-1", "After-filter 1 ran") }
        app.after { ctx -> ctx.header("X-AFTER-2", "After-filter 2 ran") }
        app.get("/mapped", TestUtil.okHandler)
        assertThat(http.get("/maped").headers.getFirst("X-BEFORE-1"), `is`("Before-filter 1 ran"))
        assertThat(http.get("/maped").headers.getFirst("X-BEFORE-2"), `is`("Before-filter 2 ran"))
        assertThat(http.get("/maped").headers.getFirst("X-AFTER-1"), `is`("After-filter 1 ran"))
        assertThat(http.get("/maped").headers.getFirst("X-AFTER-2"), `is`("After-filter 2 ran"))
    }

    @Test
    fun `after-handler sets header after endpoint-handler`() = TestUtil.test { app, http ->
        app.get("/mapped", TestUtil.okHandler)
        app.after { ctx -> ctx.header("X-AFTER", "After-filter ran") }
        assertThat(http.get("/mapped").headers.getFirst("X-AFTER"), `is`("After-filter ran"))
    }

    @Test
    fun `after is after before`() = TestUtil.test { app, http ->
        app.before { ctx -> ctx.header("X-FILTER", "This header is mine!") }
        app.after { ctx -> ctx.header("X-FILTER", "After-filter beats before-filter") }
        app.get("/mapped", TestUtil.okHandler)
        assertThat(http.get("/maped").headers.getFirst("X-FILTER"), `is`("After-filter beats before-filter"))
    }

    @Test
    fun `before-handler can add trailing slashes`() = TestUtil.test(Javalin.create().dontIgnoreTrailingSlashes()) { app, http ->
        app.before { ctx ->
            if (!ctx.path().endsWith("/")) {
                ctx.redirect(ctx.path() + "/")
            }
        }
        app.get("/ok/", TestUtil.okHandler)
        assertThat(http.getBody("/ok"), `is`("OK"))
    }

}
