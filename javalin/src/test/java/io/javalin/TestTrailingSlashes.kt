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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URLEncoder

class TestTrailingSlashes {
    private val okHttp = OkHttpClient().newBuilder().build()
    fun OkHttpClient.getBody(path: String) = this.newCall(Request.Builder().url(path).get().build()).execute().body()!!.string()
    val javalin = Javalin.create { it.ignoreTrailingSlashes = false; }

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
        TestUtil.test(javalin) { app, http ->
            app.get("/a") { ctx -> ctx.result("a") }
            app.get("/a/") { ctx -> ctx.result("a-slash") }
            assertThat(http.getBody("/a")).isEqualTo("a")
            assertThat(http.getBody("/a/")).isEqualTo("a-slash")
        }
    }

    @Test
    fun `trailing slashes don't change url params behaviour`() {
        TestUtil.test(javalin) { app, http ->
            app.get("/a") { ctx -> ctx.result("a") }
            app.get("/a/") { ctx -> ctx.result("a-slash") }
            assertThat(http.getBody("/a")).isEqualTo("a")
            assertThat(http.getBody("/a/")).isEqualTo("a-slash")
        }
    }

    @Test
    fun `wildcard first works`() = TestUtil.test(javalin) { app, http ->
        app.get("/*/test") { it.result("!") }
        app.get("/*/test/") { it.result("!/") }
        assertThat(http.getBody("/en/test")).isEqualTo("!")
        assertThat(http.getBody("/en/test/")).isEqualTo("!/")
    }

    @Test
    fun `wildcard middle works`() = TestUtil.test(javalin) { app, http ->
        app.get("/test/*/test") { it.result("!") }
        app.get("/test/*/test/") { it.result("!/") }
        assertThat(http.getBody("/test/en/test")).isEqualTo("!")
        assertThat(http.getBody("/test/en/test/")).isEqualTo("!/")
    }

    @Test
    fun `wildcard end works`() = TestUtil.test(javalin) { app, http ->
        app.get("/test/*") { it.result("!") }
        app.get("/test/*/") { it.result("!/") }
        assertThat(http.getBody("/test/en")).isEqualTo("!")
        assertThat(http.getBody("/test/en/")).isEqualTo("!") //wildcard at end match all urls, so trailingSlashes are captured by *
    }

    @Test
    fun `case sensitive urls work`() = TestUtil.test(javalin) { app, http ->
        app.get("/My-Url") { ctx -> ctx.result("OK") }
        assertThat(http.getBody("/MY-URL")).isEqualTo("Not found")
        assertThat(http.getBody("/My-Url")).isEqualTo("OK")
        app.get("/My-Url/") { ctx -> ctx.result("OK/") }
        assertThat(http.getBody("/MY-URL/")).isEqualTo("Not found")
        assertThat(http.getBody("/My-Url/")).isEqualTo("OK/")
    }

    @Test
    fun `utf-8 encoded path-params work`() = TestUtil.test(javalin) { app, http ->
        app.get("/:path-param") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        app.get("/:path-param/") { ctx -> ctx.result(ctx.pathParam("path-param") + "/") }
        assertThat(okHttp.getBody(http.origin + "/" + URLEncoder.encode("TE/ST", "UTF-8"))).isEqualTo("TE/ST")
        assertThat(okHttp.getBody(http.origin + "/" + URLEncoder.encode("TE/ST/", "UTF-8"))).isEqualTo("TE/ST/")
    }

    @Test
    fun `path-params work case-sensitive`() = TestUtil.test(javalin) { app, http ->
        app.get("/:userId") { ctx -> ctx.result(ctx.pathParam("userId")) }
        assertThat(http.getBody("/path-param")).isEqualTo("path-param")
        app.get("/:a/:A") { ctx -> ctx.result("${ctx.pathParam("a")}-${ctx.pathParam("A")}") }
        assertThat(http.getBody("/a/B")).isEqualTo("a-B")

        app.get("/:userId/") { ctx -> ctx.result(ctx.pathParam("userId") + "/") }
        assertThat(http.getBody("/path-param/")).isEqualTo("path-param/")
        app.get("/:a/:A/") { ctx -> ctx.result("${ctx.pathParam("a")}-${ctx.pathParam("A")}/") }
        assertThat(http.getBody("/a/B/")).isEqualTo("a-B/")
    }

    @Test
    fun `path-param values retain their casing`() = TestUtil.test(javalin) { app, http ->
        app.get("/:path-param") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        app.get("/:path-param/") { ctx -> ctx.result(ctx.pathParam("path-param") + "/") }
        assertThat(http.getBody("/SomeCamelCasedValue/")).isEqualTo("SomeCamelCasedValue/")
        assertThat(http.getBody("/SomeCamelCasedValue/")).isEqualTo("SomeCamelCasedValue/")
    }

    @Test
    fun `path regex works`() = TestUtil.test(javalin) { app, http ->
        app.get("/:path-param/[0-9]+") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        app.get("/:path-param/[0-9]+/") { ctx -> ctx.result(ctx.pathParam("path-param") + "/") }
        assertThat(http.getBody("/test/pathParam")).isEqualTo("Not found")
        assertThat(http.getBody("/test/21")).isEqualTo("test")
        assertThat(http.getBody("/test/pathParam/")).isEqualTo("Not found")
        assertThat(http.getBody("/test/21/")).isEqualTo("test/")
    }

    @Test
    fun `automatic slash prefixing works`() = TestUtil.test(javalin) { app, http ->
        app.routes {
            path("test") {
                path(":id") { get { ctx -> ctx.result(ctx.pathParam("id")) } }
                path(":id/") { get { ctx -> ctx.result(ctx.pathParam("id") + "/") } }
                get { ctx -> ctx.result("test") }
            }
        }
        assertThat(http.getBody("/test/path-param")).isEqualTo("path-param")
        assertThat(http.getBody("/test/path-param/")).isEqualTo("path-param/")
        assertThat(http.getBody("/test/")).isEqualTo("Not found")
        assertThat(http.getBody("/test")).isEqualTo("test")
    }

}
