/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.core.util.Header
import io.javalin.http.util.CookieStore
import io.javalin.plugin.rendering.template.TemplateUtil
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*

class TestCookieStore {

    @Test
    fun `cookieStore works between two handlers`() = TestUtil.test { app, http ->
        app.get("/cookie-store") { ctx -> ctx.cookieStore("test-object", 123) }
        app.after("/cookie-store") { ctx ->
            if (ctx.cookieStore<Any>("test-object") is Int) {
                ctx.result("Got stored value from different handler")
            }
        }
        assertThat(http.getBody("/cookie-store")).isEqualTo("Got stored value from different handler")
    }

    @Test
    fun `cookieStore can be cleared`() = TestUtil.test { app, http ->
        app.get("/cookie-storer") { ctx -> ctx.cookieStore("test-object", 123) }
        app.get("/cookie-clearer") { it.clearCookieStore() }
        app.get("/cookie-checker") { ctx -> ctx.result("stored: " + ctx.cookie("javalin-cookie-store")) }
        http.getBody("/cookie-storer")
        http.getBody("/cookie-clearer")
        assertThat(http.getBody("/cookie-checker")).isEqualTo("stored: null")
    }

    @Test
    fun `cookieStore works between two requests`() = TestUtil.test { app, http ->
        app.get("/cookie-storer") { ctx -> ctx.cookieStore("test-object", 123) }
        app.get("/cookie-reader") { ctx ->
            if (ctx.cookieStore<Any>("test-object") is Int) {
                ctx.result("Got stored value from different request")
            }
        }
        http.getBody("/cookie-storer")
        assertThat(http.getBody("/cookie-reader")).isEqualTo("Got stored value from different request")
    }

    @Test
    fun `cookieStore works between two request with object overwrite`() = TestUtil.test { app, http ->
        app.get("/cookie-storer") { ctx -> ctx.cookieStore("test-object", 1) }
        app.get("/cookie-overwriter") { ctx -> ctx.cookieStore("test-object", "Hello world!") }
        app.get("/cookie-reader") { ctx ->
            if ("Hello world!" == ctx.cookieStore<Any>("test-object")) {
                ctx.result("Overwrote cookie from previous request")
            }
        }
        http.getBody("/cookie-storer")
        http.getBody("/cookie-overwriter")
        assertThat(http.getBody("/cookie-reader")).isEqualTo("Overwrote cookie from previous request")
    }

    @Test
    fun `cookieStore works between two request with multiple objects`() = TestUtil.test { app, http ->
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
        assertThat(http.getBody("/cookie-reader")).isEqualTo("Hello world! 42 42.0 [One, Two, Three] {K1=V, K2=1000.0, K3=[One, Two, Three]}")
    }

    @Test
    fun `cookieStore cookie path is root`() = TestUtil.test { app, http ->
        app.get("/") { it.cookieStore("s", "Hello world!") }
        assertThat(http.get("/").headers.getFirst(Header.SET_COOKIE)).endsWith("; Path=/")
    }

    @Test
    fun `renaming cookieStore cookie works`() = TestUtil.test { app, http ->
        app.get("/") { it.cookieStore("s", "Hello world!") }
        assertThat(http.get("/").headers.getFirst(Header.SET_COOKIE)).startsWith(CookieStore.COOKIE_NAME)
        CookieStore.COOKIE_NAME = "another-name"
        assertThat(http.get("/").headers.getFirst(Header.SET_COOKIE)).startsWith(CookieStore.COOKIE_NAME)
    }

}
