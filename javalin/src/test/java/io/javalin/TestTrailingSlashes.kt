/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestTrailingSlashes {

    @Test
    fun `trailing slashes are ignored by default`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.result("Hello, slash!") }
        assertThat(http.getBody("/hello")).isEqualTo("Hello, slash!")
        assertThat(http.getBody("/hello/")).isEqualTo("Hello, slash!")
    }

    @Test
    fun `trailing slashes are ignored by default - ApiBuilder`() = TestUtil.test { app, http ->
        app.routes {
            path("a") {
                get { ctx -> ctx.result("a") }
                get("/") { ctx -> ctx.result("a-slash") }
            }
        }
        assertThat(http.getBody("/a")).isEqualTo("a")
        assertThat(http.getBody("/a/")).isEqualTo("a")
    }

    @Test
    fun `trailing slashes are treat as different url, if configuration is set - ApiBuilder`() {
        val javalin = Javalin.create { it.ignoreTrailingSlashes = false; }
        TestUtil.test(javalin) { app, http ->
            app.get("/a") { ctx -> ctx.result("a") }
            app.get("/a/") { ctx -> ctx.result("a-slash") }
            assertThat(http.getBody("/a")).isEqualTo("a")
            assertThat(http.getBody("/a/")).isEqualTo("a-slash")
        }
    }

}
