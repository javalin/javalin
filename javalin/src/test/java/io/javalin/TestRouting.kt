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

class TestRouting {

    private val okHttp = OkHttpClient().newBuilder().build()
    fun OkHttpClient.getBody(path: String) = this.newCall(Request.Builder().url(path).get().build()).execute().body()!!.string()

    @Test
    fun `wildcard first works`() = TestUtil.test { app, http ->
        app.get("/*/test") { it.result("!") }
        assertThat(http.getBody("/en/test")).isEqualTo("!")
    }

    @Test
    fun `wildcard middle works`() = TestUtil.test { app, http ->
        app.get("/test/*/test") { it.result("!") }
        assertThat(http.getBody("/test/en/test")).isEqualTo("!")
    }

    @Test
    fun `wildcard end works`() = TestUtil.test { app, http ->
        app.get("/test/*") { it.result("!") }
        assertThat(http.getBody("/test/en")).isEqualTo("!")
    }

    @Test
    fun `case sensitive urls work`() = TestUtil.test { app, http ->
        app.get("/My-Url") { ctx -> ctx.result("OK") }
        assertThat(http.getBody("/MY-URL")).isEqualTo("Not found")
        assertThat(http.getBody("/My-Url")).isEqualTo("OK")
    }

    @Test
    fun `utf-8 encoded path-params work`() = TestUtil.test { app, http ->
        app.get("/:path-param") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        assertThat(okHttp.getBody(http.origin + "/" + URLEncoder.encode("TE/ST", "UTF-8"))).isEqualTo("TE/ST")
    }

    @Test
    fun `path-params work case-sensitive`() = TestUtil.test { app, http ->
        app.get("/:userId") { ctx -> ctx.result(ctx.pathParam("userId")) }
        assertThat(http.getBody("/path-param")).isEqualTo("path-param")
        app.get("/:a/:A") { ctx -> ctx.result("${ctx.pathParam("a")}-${ctx.pathParam("A")}") }
        assertThat(http.getBody("/a/B")).isEqualTo("a-B")
    }

    @Test
    fun `path-param values retain their casing`() = TestUtil.test { app, http ->
        app.get("/:path-param") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        assertThat(http.getBody("/SomeCamelCasedValue")).isEqualTo("SomeCamelCasedValue")
    }

    @Test
    fun `path regex works`() = TestUtil.test { app, http ->
        app.get("/:path-param/[0-9]+/") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        assertThat(http.getBody("/test/pathParam")).isEqualTo("Not found")
        assertThat(http.getBody("/test/21")).isEqualTo("test")
    }

    @Test
    fun `automatic slash prefixing works`() = TestUtil.test { app, http ->
        app.routes {
            path("test") {
                path(":id") { get { ctx -> ctx.result(ctx.pathParam("id")) } }
                get { ctx -> ctx.result("test") }
            }
        }
        assertThat(http.getBody("/test/path-param/")).isEqualTo("path-param")
        assertThat(http.getBody("/test/")).isEqualTo("test")
    }

}
