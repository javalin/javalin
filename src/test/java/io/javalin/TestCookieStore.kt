/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.rendering.template.TemplateUtil
import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test
import java.util.*

class TestCookieStore {

    @Test
    fun `cookieStore() works between two handlers`() = TestUtil.test { app, http ->
        app.get("/cookie-store") { ctx -> ctx.cookieStore("test-object", 123) }
        app.after("/cookie-store") { ctx ->
            if (ctx.cookieStore<Any>("test-object") is Int) {
                ctx.result("Got stored value from different handler")
            }
        }
        assertThat(http.getBody("/cookie-store"), `is`("Got stored value from different handler"))
    }

    @Test
    fun `cookieStore() can be cleared`() = TestUtil.test { app, http ->
        app.get("/cookie-storer") { ctx -> ctx.cookieStore("test-object", 123) }
        app.get("/cookie-clearer") { it.clearCookieStore() }
        app.get("/cookie-checker") { ctx -> ctx.result("stored: " + ctx.cookie("javalin-cookie-store")) }
        http.getBody("/cookie-storer")
        http.getBody("/cookie-clearer")
        assertThat(http.getBody("/cookie-checker"), `is`("stored: null"))
    }

    @Test
    fun `cookieStore() works between two requests`() = TestUtil.test { app, http ->
        app.get("/cookie-storer") { ctx -> ctx.cookieStore("test-object", 123) }
        app.get("/cookie-reader") { ctx ->
            if (ctx.cookieStore<Any>("test-object") is Int) {
                ctx.result("Got stored value from different request")
            }
        }
        http.getBody("/cookie-storer")
        assertThat(http.getBody("/cookie-reader"), `is`("Got stored value from different request"))
    }

    @Test
    fun `cookieStore() works between two request with object overwrite`() = TestUtil.test { app, http ->
        app.get("/cookie-storer") { ctx -> ctx.cookieStore("test-object", 1) }
        app.get("/cookie-overwriter") { ctx -> ctx.cookieStore("test-object", "Hello world!") }
        app.get("/cookie-reader") { ctx ->
            if ("Hello world!" == ctx.cookieStore<Any>("test-object")) {
                ctx.result("Overwrote cookie from previous request")
            }
        }
        http.getBody("/cookie-storer")
        http.getBody("/cookie-overwriter")
        assertThat(http.getBody("/cookie-reader"), `is`("Overwrote cookie from previous request"))
    }

    @Test
    fun `cookieStore() works between two request with multiple objects`() = TestUtil.test { app, http ->
        app.get("/cookie-storer") { ctx ->
            ctx.cookieStore("s", "Hello world!")
            ctx.cookieStore("i", 42)
            ctx.cookieStore("d", 42.0)
            ctx.cookieStore("l", Arrays.asList("One", "Two", "Three"))
            ctx.cookieStore("m", TemplateUtil.model("K1", "V", "K2", 1000.0, "K3", Arrays.asList("One", "Two", "Three")))
        }
        app.get("/cookie-reader") { ctx ->
            val s = ctx.cookieStore<String>("s")
            val i = ctx.cookieStore<Int>("i")
            val d = ctx.cookieStore<Double>("d")
            val l = ctx.cookieStore<List<*>>("l")
            val m = ctx.cookieStore<Map<*, *>>("m")
            ctx.result("$s $i $d $l $m")
        }
        http.getBody("/cookie-storer")
        assertThat(http.getBody("/cookie-reader"), `is`("Hello world! 42 42.0 [One, Two, Three] {K1=V, K2=1000.0, K3=[One, Two, Three]}"))
    }

}
