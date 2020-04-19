/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.HttpMethod
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestFilters {

    @Test
    fun `filters run before root handler`() = TestUtil.test { app, http ->
        app.before { ctx -> ctx.header("X-BEFOREFILTER", "Before-filter ran") }
        app.after { ctx -> ctx.header("X-AFTERFILTER", "After-filter ran") }
        app.get("/", TestUtil.okHandler)
        assertThat(http.get("/").headers.getFirst("X-BEFOREFILTER")).isEqualTo("Before-filter ran")
        assertThat(http.get("/").headers.getFirst("X-AFTERFILTER")).isEqualTo("After-filter ran")
    }

    @Test
    fun `app returns 404 for GET if only before-handler present`() = TestUtil.test { app, http ->
        app.before(TestUtil.okHandler)
        val response = http.call(HttpMethod.GET, "/hello")
        assertThat(response.status).isEqualTo(404)
        assertThat(response.body).isEqualTo("Not found")
    }

    @Test
    fun `app returns 404 for POST if only before-handler present`() = TestUtil.test { app, http ->
        app.before(TestUtil.okHandler)
        val response = http.call(HttpMethod.POST, "/hello")
        assertThat(response.status).isEqualTo(404)
        assertThat(response.body).isEqualTo("Not found")
    }

    @Test
    fun `before-handler can set header`() = TestUtil.test { app, http ->
        app.before { ctx -> ctx.header("X-FILTER", "Before-filter ran") }
        app.get("/mapped", TestUtil.okHandler)
        assertThat(http.get("/maped").headers.getFirst("X-FILTER")).isEqualTo("Before-filter ran")
    }

    @Test
    fun `before- and after-handlers can set a bunch of headers`() = TestUtil.test { app, http ->
        app.before { ctx -> ctx.header("X-BEFORE-1", "Before-filter 1 ran") }
        app.before { ctx -> ctx.header("X-BEFORE-2", "Before-filter 2 ran") }
        app.after { ctx -> ctx.header("X-AFTER-1", "After-filter 1 ran") }
        app.after { ctx -> ctx.header("X-AFTER-2", "After-filter 2 ran") }
        app.get("/mapped", TestUtil.okHandler)
        assertThat(http.get("/maped").headers.getFirst("X-BEFORE-1")).isEqualTo("Before-filter 1 ran")
        assertThat(http.get("/maped").headers.getFirst("X-BEFORE-2")).isEqualTo("Before-filter 2 ran")
        assertThat(http.get("/maped").headers.getFirst("X-AFTER-1")).isEqualTo("After-filter 1 ran")
        assertThat(http.get("/maped").headers.getFirst("X-AFTER-2")).isEqualTo("After-filter 2 ran")
    }

    @Test
    fun `after-handler sets header after endpoint-handler`() = TestUtil.test { app, http ->
        app.get("/mapped", TestUtil.okHandler)
        app.after { ctx -> ctx.header("X-AFTER", "After-filter ran") }
        assertThat(http.get("/mapped").headers.getFirst("X-AFTER")).isEqualTo("After-filter ran")
    }

    @Test
    fun `after is after before`() = TestUtil.test { app, http ->
        app.before { ctx -> ctx.header("X-FILTER", "This header is mine!") }
        app.after { ctx -> ctx.header("X-FILTER", "After-filter beats before-filter") }
        app.get("/mapped", TestUtil.okHandler)
        assertThat(http.get("/maped").headers.getFirst("X-FILTER")).isEqualTo("After-filter beats before-filter")
    }

}
