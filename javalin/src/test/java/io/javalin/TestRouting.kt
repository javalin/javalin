/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.HttpMethod
import io.javalin.apibuilder.ApiBuilder.after
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.core.routing.MissingBracketsException
import io.javalin.core.routing.ParameterNamesNotUniqueException
import io.javalin.core.routing.WildcardBracketAdjacentException
import io.javalin.testing.TestUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URLEncoder

class TestRouting {

    private val okHttp = OkHttpClient().newBuilder().build()
    fun OkHttpClient.getBody(path: String) = this.newCall(Request.Builder().url(path).get().build()).execute().body!!.string()

    @Test
    fun `basic hello world works`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.result("Hello World") }
        assertThat(http.getBody("/hello")).isEqualTo("Hello World")
    }

    @Test
    fun `all mapped verbs return 200`() = TestUtil.test { app, http ->
        app.get("/mapped", TestUtil.okHandler)
        app.post("/mapped", TestUtil.okHandler)
        app.put("/mapped", TestUtil.okHandler)
        app.delete("/mapped", TestUtil.okHandler)
        app.patch("/mapped", TestUtil.okHandler)
        app.head("/mapped", TestUtil.okHandler)
        app.options("/mapped", TestUtil.okHandler)
        for (httpMethod in HttpMethod.values()) {
            assertThat(http.call(httpMethod, "/mapped").status).isEqualTo(200)
        }
    }

    @Test
    fun `all unmapped verbs return 404`() = TestUtil.test { _, http ->
        for (httpMethod in HttpMethod.values()) {
            assertThat(http.call(httpMethod, "/unmapped").status).isEqualTo(404)
        }
    }

    @Test
    fun `HEAD returns 200 if GET is mapped`() = TestUtil.test { app, http ->
        app.get("/mapped", TestUtil.okHandler)
        assertThat(http.call(HttpMethod.HEAD, "/mapped").status).isEqualTo(200)
    }

    @Test
    fun `urls are case sensitive`() = TestUtil.test { app, http ->
        app.get("/My-Url") { ctx -> ctx.result("OK") }
        assertThat(http.get("/My-Url").status).isEqualTo(200)
        assertThat(http.get("/MY-URL").status).isEqualTo(404)
        assertThat(http.get("/my-url").status).isEqualTo(404)
    }

    @Test
    fun `filers are executed in order`() = TestUtil.test { app, http ->
        app.before { ctx -> ctx.result("1") }
        app.before { ctx -> ctx.result(ctx.resultString() + "2") }
        app.get("/hello") { ctx -> ctx.result(ctx.resultString() + "Hello") }
        app.after { ctx -> ctx.result(ctx.resultString() + "3") }
        assertThat(http.getBody("/hello")).isEqualTo("12Hello3")
    }

    @Test
    fun `old style colon path parameter throws exception`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy { Javalin.create().get("/:test") {} }
            .withMessageStartingWith("Path '/:test' invalid - Javalin 4 switched from ':param' to '{param}'.")
    }

    @Test
    fun `literal colon in path segment works`() = TestUtil.test { app, http ->
        app.get("/hello:world") { ctx -> ctx.result("Hello World") }
        assertThat(http.getBody("/hello:world")).isEqualTo("Hello World")
    }

    @Test
    fun `wildcard first works`() = TestUtil.test { app, http ->
        app.get("/*/test") { it.result("!") }
        assertThat(http.getBody("/tast/test")).isEqualTo("!")
    }

    @Test
    fun `wildcard last works`() = TestUtil.test { app, http ->
        app.get("/test/*") { it.result("!") }
        assertThat(http.getBody("/test")).isEqualTo("Not found")
        assertThat(http.getBody("/test/1")).isEqualTo("!")
        assertThat(http.getBody("/test/tast")).isEqualTo("!")
    }

    @Test
    fun `wildcard middle works`() = TestUtil.test { app, http ->
        app.get("/test/*/test") { it.result("!") }
        assertThat(http.getBody("/test/en/test")).isEqualTo("!")
    }

    @Test
    fun `path params work`() = TestUtil.test { app, http ->
        app.get("/{p1}") { it.result(it.pathParamMap.toString()) }
        assertThat(http.getBody("/param1")).isEqualTo("{p1=param1}")
    }

    @Test
    fun `can add multiple path params in same path segment`() = TestUtil.test { app, http ->
        app.get("/{p1}AND{p2}") { it.result(it.pathParamMap.toString()) }
        assertThat(http.getBody("/param1ANDparam2")).isEqualTo("{p1=param1, p2=param2}")
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
    fun `percentage operator does not consume text`() = TestUtil.test { app, http ->
        app.get("/{name}*") { ctx -> ctx.result(ctx.pathParam("name")) }
        assertThat(http.getBody("/text")).isEqualTo("text")
        assertThat(http.getBody("/text/two")).isEqualTo("text")
    }

    @Test
    fun `path-params cannot directly follow a wildcard`() = TestUtil.test { app, _ ->
        assertThrows<WildcardBracketAdjacentException> {
            app.get("/*{name}") { ctx -> ctx.result(ctx.pathParam("name")) }
        }
    }

    @Test
    fun `angle-bracket path-params can accept slashes`() = TestUtil.test { app, http ->
        app.get("/<name>") { ctx -> ctx.result(ctx.pathParam("name")) }
        assertThat(http.getBody("/hi/with/slashes")).isEqualTo("hi/with/slashes")
    }

    @Test
    fun `angle-bracket path-params can be combined with regular content`() = TestUtil.test { app, http ->
        app.get("/hi/<name>") { ctx -> ctx.result(ctx.pathParam("name")) }
        assertThat(http.getBody("/hi/with/slashes")).isEqualTo("with/slashes")
    }

    @Test
    fun `angle-bracket path-params can be combined with wildcards`() = TestUtil.test { app, http ->
        app.get("/hi-<name>-*") { ctx -> ctx.result(ctx.pathParam("name")) }
        assertThat(http.get("/hi-world").status).isEqualTo(404)
        val response = http.get("/hi-world/hi-not-included")
        assertThat(response.status).isEqualTo(200)
        assertThat(response.body).isEqualTo("world/hi")
    }

    // looking for a solution to enable this on a per-path basis
    @Disabled
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
                path("{id}") {
                    get { ctx -> ctx.result(ctx.pathParam("id")) }
                }
                get { ctx -> ctx.result("test") }
            }
        }
        assertThat(http.getBody("/test/path-param/")).isEqualTo("path-param")
        assertThat(http.getBody("/test/")).isEqualTo("test")
    }

    @Test
    fun `non sub-path star wildcard works for plain paths`() = TestUtil.test { app, http ->
        app.get("/p") { it.result("1") }.also { assertThat(http.getBody("/p")).isEqualTo("1") }
        app.get("/p-test") { it.result("2") }.also { assertThat(http.getBody("/p-test")).isEqualTo("2") }
        app.after("/p*") { it.result("${it.resultString()}AFTER") }.also {
            assertThat(http.getBody("/p")).isEqualTo("1AFTER")
            assertThat(http.getBody("/p-test")).isEqualTo("2AFTER")
        }
    }

    @Test
    fun `non sub-path wildcard works for path-params`() = TestUtil.test { app, http ->
        app.get("/{pp}-test") { it.result("2") }.also { assertThat(http.getBody("/p-test")).isEqualTo("2") }
        app.get("/{pp}") { it.result("1") }.also { assertThat(http.getBody("/p")).isEqualTo("1") }
        app.after("/{pp}*") { it.result("${it.resultString()}AFTER") }.also {
            assertThat(http.getBody("/p")).isEqualTo("1AFTER")
            assertThat(http.getBody("/p-test")).isEqualTo("2AFTER")
        }
    }

    @Test
    fun `sub-path wildcard works for path-params`() = TestUtil.test { app, http ->
        app.routes {
            after("/partners/{pp}*") { it.result("${it.resultString()} - after") }
            path("/partners/{pp}") {
                get { it.result("root") }
                get("/api") { it.result("api") }
            }
        }
        assertThat(http.getBody("/partners/microsoft")).isEqualTo("root - after")
        assertThat(http.getBody("/partners/microsoft/api")).isEqualTo("api - after")
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

}
