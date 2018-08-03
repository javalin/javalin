/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test

class TestTrailingSlashes {

    private val nonIgnoringJavalin = Javalin.create().dontIgnoreTrailingSlashes()

    @Test
    fun `doesn't ignore when configured (instance)`() = TestUtil.test(nonIgnoringJavalin) { app, http ->
        app.get("/hello") { ctx -> ctx.result("Hello, slash!") }
        assertThat(http.getBody("/hello"), `is`("Hello, slash!"))
        assertThat(http.getBody("/hello/"), `is`("Not found"))
    }

    @Test
    fun `doesn't ignore when configured (ApiBuilder)`() = TestUtil.test(nonIgnoringJavalin) { app, http ->
        app.routes {
            path("a") {
                get { ctx -> ctx.result("a") }
                get("/") { ctx -> ctx.result("a-slash") }
            }
        }
        assertThat(http.getBody("/a"), `is`("a"))
        assertThat(http.getBody("/a/"), `is`("a-slash"))
    }

    @Test
    fun `ignores by default (instance)`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.result("Hello, slash!") }
        assertThat(http.getBody("/hello"), `is`("Hello, slash!"))
        assertThat(http.getBody("/hello/"), `is`("Hello, slash!"))
    }

    @Test
    fun `ignores by default (ApiBuilder)`() = TestUtil.test { app, http ->
        app.routes {
            path("a") {
                get { ctx -> ctx.result("a") }
                get("/") { ctx -> ctx.result("a-slash") }
            }
        }
        assertThat(http.getBody("/a"), `is`("a"))
        assertThat(http.getBody("/a/"), `is`("a"))
    }

}
