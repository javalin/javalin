/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.core.routing.MissingBracketsException
import io.javalin.core.routing.ParameterNamesNotUniqueException
import io.javalin.core.routing.WildcardBracketAdjacentException
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test
import java.net.URLEncoder

class TestRouting {

    private val okHttp = OkHttpClient().newBuilder().build()
    fun OkHttpClient.getBody(path: String) = this.newCall(Request.Builder().url(path).get().build()).execute().body!!.string()

    @Test
    fun `colon in path throws exception`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { Javalin.create().get("/:test") {} }
            .withMessageStartingWith("Path '/:test' invalid - Javalin 4 switched from ':param' to '{param}'.")
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

    @Test
    fun `double star does not consume text`() = TestUtil.test { app, http ->
        app.get("/{name}**") { ctx -> ctx.result(ctx.pathParam("name")) }
        assertThat(http.getBody("/text")).isEqualTo("text")
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

    @Test
    fun `non sub-path wildcard works for paths`() = TestUtil.test { app, http ->
        app.get("/p") { it.result("GET") }
        app.get("/p/test") { it.result("GET") }
        assertThat(http.getBody("/p")).isEqualTo("GET")
        assertThat(http.getBody("/p/test")).isEqualTo("GET")
        app.after("/p**") { it.result((it.resultString() ?: "") + "AFTER") }
        assertThat(http.getBody("/p")).isEqualTo("GETAFTER")
        assertThat(http.getBody("/p/test")).isEqualTo("GETAFTER")
    }

    @Test
    fun `non sub-path wildcard works for path-params`() = TestUtil.test { app, http ->
        app.get("/{pp}") { it.result(it.resultString() + it.pathParam("pp")) }
        app.get("/{pp}/test") { it.result(it.resultString() + it.pathParam("pp")) }
        assertThat(http.getBody("/123")).isEqualTo("null123")
        assertThat(http.getBody("/123/test")).isEqualTo("null123")
        app.before("/{pp}**") { it.result("BEFORE") }
        assertThat(http.getBody("/123")).isEqualTo("BEFORE123")
        assertThat(http.getBody("/123/test")).isEqualTo("BEFORE123")
    }

    @Test
    fun `path param names are required to be unique across path param types`() = TestUtil.test { app, _ ->
        assertThatExceptionOfType(ParameterNamesNotUniqueException::class.java).isThrownBy {
            app.get("/{param}/demo/<param>") { ctx -> ctx.result(ctx.pathParam("param")) }
        }
    }

    @Test
    fun `missing brackets lead to an exception`() = TestUtil.test { app, _ ->
        listOf(
            "/{",
            "/}",
            "/>",
            "/<",
            "/</>"
        ).forEach {
            assertThatExceptionOfType(MissingBracketsException::class.java).describedAs(it).isThrownBy {
                app.get(it) { ctx -> ctx.result("") }
            }
        }
    }

    @Test
    fun `proposal works as expected`() = TestUtil.test { app, http ->
        app.get("/a-*") { ctx -> ctx.result("A") }
        app.get("/b-**") { ctx -> ctx.result("B") }
        app.get("/c-<param>") { ctx -> ctx.result("C" + ctx.pathParam("param")) }
        app.get("/d-{param}") { ctx -> ctx.result("D" + ctx.pathParam("param")) }
        app.get("/e-<param>-end") { ctx -> ctx.result("E" + ctx.pathParam("param")) }
        app.get("/f-{param}-end") { ctx -> ctx.result("F" + ctx.pathParam("param")) }
        app.get("/g-***") { ctx -> ctx.result("G") }

        proposalAssertions200(http)
        nonMatchingAssertions(http)
    }

    private fun proposalAssertions200(http: HttpUtil) {
        listOf(
            "/a-" to "A", // this one is unexpected?
            "/a-wildcard" to "A",
            "/a-/other" to "A", // this one is unexpected?
            "/b-" to "B",
            "/b-/other" to "B",
            "/c-hi" to "Chi",
            "/c-with/slashes" to "Cwith/slashes",
            "/d-hi" to "Dhi",
            "/e-hi-end" to "Ehi",
            "/e-with/slashes-end" to "Ewith/slashes",
            "/f-hi-end" to "Fhi",
            "/g-" to "G",
            "/g-wildcard" to "G",
            "/g-/other" to "G",
        ).forEach { (path, body) ->
            val response = http.get(path)
            assertThat(response.status).`as`("$path - status").isEqualTo(200)
            assertThat(response.body).`as`("$path - body").isEqualTo(body)
        }
    }

    private fun nonMatchingAssertions(http: HttpUtil) {
        listOf(
            "/b-aa",
            "/c-",
            "/d-",
            "/e-",
            "/f-",
        ).forEach {
            val response = http.get(it)
            assertThat(response.status).`as`("$it - 404").isEqualTo(404)
        }
    }
}
