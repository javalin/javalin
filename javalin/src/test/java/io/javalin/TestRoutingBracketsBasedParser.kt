/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.core.JavalinPathParser
import io.javalin.core.WildcardBracketAdjacentException
import io.javalin.testing.TestUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import java.net.URLEncoder

class TestRoutingBracketsBasedParser {

    private val okHttp = OkHttpClient().newBuilder().build()
    fun OkHttpClient.getBody(path: String) = this.newCall(Request.Builder().url(path).get().build()).execute().body()!!.string()

    @Before
    fun setup() {
        JavalinPathParser.useBracketsBasedParser()
    }

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
        assertThat(http.get("/MY-URL").status).isEqualTo(404)
        val response = http.get("/My-Url")
        assertThat(response.status).isEqualTo(200)
        assertThat(response.body).isEqualTo("OK")
    }

    @Test
    fun `utf-8 encoded path-params work`() = TestUtil.test { app, http ->
        app.get("/{path-param}") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        assertThat(okHttp.getBody(http.origin + "/" + URLEncoder.encode("TE/ST", "UTF-8"))).isEqualTo("TE/ST")
    }

    @Test
    fun `path-params work case-sensitive`() = TestUtil.test { app, http ->
        app.get("/{userId}") { ctx -> ctx.result(ctx.pathParam("userId")) }
        assertThat(http.getBody("/path-param")).isEqualTo("path-param")
        app.get("/{a}/{A}") { ctx -> ctx.result("${ctx.pathParam("a")}-${ctx.pathParam("A")}") }
        assertThat(http.getBody("/a/B")).isEqualTo("a-B")
    }

    @Test
    fun `path-param values retain their casing`() = TestUtil.test { app, http ->
        app.get("/{path-param}") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        assertThat(http.getBody("/SomeCamelCasedValue")).isEqualTo("SomeCamelCasedValue")
    }

    @Test
    fun `path-params can be combined with regular content`() = TestUtil.test { app, http ->
        app.get("/hi-{name}") { ctx -> ctx.result(ctx.pathParam("name")) }
        assertThat(http.getBody("/hi-world")).isEqualTo("world")
    }

    @Test
    fun `path-params can be combined with wildcards`() = TestUtil.test { app, http ->
        app.get("/hi-{name}-*") { ctx -> ctx.result(ctx.pathParam("name")) }
        assertThat(http.get("/hi-world").status).isEqualTo(404)
        val response = http.get("/hi-world-not-included")
        assertThat(response.status).isEqualTo(200)
        assertThat(response.body).isEqualTo("world")
    }

    @Test
    fun `path-params support stars in names`() = TestUtil.test { app, http ->
        app.get("/hi-{name*}") { ctx -> ctx.result(ctx.pathParam("name*")) }
        assertThat(http.getBody("/hi-world")).isEqualTo("world")
    }

    @Test(expected = WildcardBracketAdjacentException::class)
    fun `path-params cannot directly be followed with a wildcard`() = TestUtil.test { app, _ ->
        app.get("/{name}*") { ctx -> ctx.result(ctx.pathParam("name")) }
    }

    @Test(expected = WildcardBracketAdjacentException::class)
    fun `path-params cannot directly follow a wildcard`() = TestUtil.test { app, _ ->
        app.get("/*{name}") { ctx -> ctx.result(ctx.pathParam("name")) }
    }

    @Test
    fun `wildcards and path-params are compatible without glue`() = TestUtil.test { app, http ->
        app.get("/hi-{name*}") { ctx -> ctx.result(ctx.pathParam("name*")) }
        assertThat(http.getBody("/hi-world")).isEqualTo("world")
    }

    @Test
    fun `splat path-params work`() = TestUtil.test { app, http ->
        app.get("/<name>") { ctx -> ctx.result(ctx.pathParam("name")) }
        assertThat(http.getBody("/hi/with/slashes")).isEqualTo("hi/with/slashes")
    }

    @Test
    fun `splat path-params can be combined with regular content`() = TestUtil.test { app, http ->
        app.get("/hi/<name>") { ctx -> ctx.result(ctx.pathParam("name")) }
        assertThat(http.getBody("/hi/with/slashes")).isEqualTo("with/slashes")
    }

    @Test
    fun `splat path-params can be combined with wildcards`() = TestUtil.test { app, http ->
        app.get("/hi-<name>-*") { ctx -> ctx.result(ctx.pathParam("name")) }
        assertThat(http.get("/hi-world").status).isEqualTo(404)
        val response = http.get("/hi-world/hi-not-included")
        assertThat(response.status).isEqualTo(200)
        assertThat(response.body).isEqualTo("world/hi")
    }

    @Test
    fun `path regex works`() = TestUtil.test { app, http ->
        app.get("/{path-param}/[0-9]+/") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        assertThat(http.get("/test/pathParam").status).isEqualTo(404)
        assertThat(http.get("/test/21").body).isEqualTo("test")
    }

    @Test
    fun `automatic slash prefixing works`() = TestUtil.test { app, http ->
        app.routes {
            path("test") {
                path("{id}") { get { ctx -> ctx.result(ctx.pathParam("id")) } }
                get { ctx -> ctx.result("test") }
            }
        }
        assertThat(http.getBody("/test/path-param/")).isEqualTo("path-param")
        assertThat(http.getBody("/test/")).isEqualTo("test")
    }
}
