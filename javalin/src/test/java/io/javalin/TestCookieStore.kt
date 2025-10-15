/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.http.Header
import io.javalin.http.util.CookieStore
import io.javalin.testing.TestUtil
import io.javalin.testing.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class TestCookieStore {

    @Test
    fun `cookieStore works between two handlers`() = TestUtil.test { app, http ->
        app.get("/cookie-store") { it.cookieStore().set("test-object", 123) }
        app.after("/cookie-store") { ctx ->
            if (ctx.cookieStore().get<Any>("test-object") is Int) {
                ctx.result("Got stored value from different handler")
            }
        }
        assertThat(http.getBody("/cookie-store")).isEqualTo("Got stored value from different handler")
    }

    @Test
    fun `cookieStore can be cleared`() = TestUtil.test { app, http ->
        app.get("/cookie-storer") { it.cookieStore().set("test-object", 123) }
        app.get("/cookie-clearer") { it.cookieStore().clear() }
        app.get("/cookie-checker") { it.result("stored: " + it.cookie("javalin-cookie-store")) }
        http.getBody("/cookie-storer")
        http.getBody("/cookie-clearer")
        assertThat(http.getBody("/cookie-checker")).isEqualTo("stored: null")
    }

    @Test
    fun `cookieStore works between two requests`() = TestUtil.test { app, http ->
        app.get("/cookie-storer") { it.cookieStore().set("test-object", 123) }
        app.get("/cookie-reader") { ctx ->
            if (ctx.cookieStore().get<Any>("test-object") is Int) {
                ctx.result("Got stored value from different request")
            }
        }
        http.getBody("/cookie-storer")
        assertThat(http.getBody("/cookie-reader")).isEqualTo("Got stored value from different request")
    }

    @Test
    fun `cookieStore works between two request with object overwrite`() = TestUtil.test { app, http ->
        app.get("/cookie-storer") { it.cookieStore().set("test-object", 1) }
        app.get("/cookie-overwriter") { it.cookieStore().set("test-object", "Hello world!") }
        app.get("/cookie-reader") { ctx ->
            if ("Hello world!" == ctx.cookieStore().get<Any>("test-object")) {
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
            ctx.cookieStore().set("s", "Hello world!")
            ctx.cookieStore().set("i", 42)
            ctx.cookieStore().set("d", 42.0)
            ctx.cookieStore().set("l", Arrays.asList("One", "Two", "Three"))
            ctx.cookieStore().set("m", mapOf("K1" to "V", "K2" to 1000.0, "K3" to Arrays.asList("One", "Two", "Three")))
        }
        app.get("/cookie-reader") { ctx ->
            val s = ctx.cookieStore().get<String>("s")
            val i = ctx.cookieStore().get<Int>("i")
            val d = ctx.cookieStore().get<Double>("d")
            val l = ctx.cookieStore().get<List<*>>("l")
            val m = ctx.cookieStore().get<Map<*, *>>("m")
            ctx.result("$s $i $d $l $m")
        }
        http.getBody("/cookie-storer")
        assertThat(http.getBody("/cookie-reader")).isEqualTo("Hello world! 42 42.0 [One, Two, Three] {K1=V, K2=1000.0, K3=[One, Two, Three]}")
    }

    @Test
    fun `cookieStore cookie path is root`() = TestUtil.test { app, http ->
        app.get("/") { it.cookieStore().set("s", "Hello world!") }
        assertThat(http.get("/").headers.getFirst(Header.SET_COOKIE)).endsWith("; Path=/")
    }

    @Test
    fun `renaming cookieStore cookie works`() = TestUtil.test { app, http ->
        app.get("/") { it.cookieStore().set("s", "Hello world!") }
        assertThat(http.get("/").headers.getFirst(Header.SET_COOKIE)).startsWith(CookieStore.COOKIE_NAME)
        CookieStore.COOKIE_NAME = "another-name"
        assertThat(http.get("/").headers.getFirst(Header.SET_COOKIE)).startsWith(CookieStore.COOKIE_NAME)
    }

    @Test
    fun `cookieStore cookie should not be duplicated`() = TestUtil.test { app, http ->
        app.get("/test") { ctx ->
            ctx.cookieStore().set("first", "hello")
            ctx.cookieStore().set("second", "world")
        }
        assertThat(http.get("/test").headers.get("Set-Cookie")).hasSize(1)
    }
}
