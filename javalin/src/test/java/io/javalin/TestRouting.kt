/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.apibuilder.ApiBuilder.after
import io.javalin.apibuilder.ApiBuilder.get
import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.OK
import io.javalin.routing.MissingBracketsException
import io.javalin.routing.ParameterNamesNotUniqueException
import io.javalin.routing.WildcardBracketAdjacentException
import io.javalin.http.HandlerType.TRACE
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import kong.unirest.HttpMethod
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
        app.get("/hello") { it.result("Hello World") }
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
        app.addHandler(TRACE, "/mapped", TestUtil.okHandler)

        for (httpMethod in HttpMethod.all()) {
            assertThat(http.call(httpMethod, "/mapped").httpCode()).isEqualTo(OK)
        }
    }

    @Test
    fun `all unmapped verbs return 404`() = TestUtil.test { _, http ->
        for (httpMethod in HttpMethod.all()) {
            assertThat(http.call(httpMethod, "/unmapped").httpCode()).isEqualTo(NOT_FOUND)
        }
    }

    @Test
    fun `HEAD returns 200 if GET is mapped`() = TestUtil.test { app, http ->
        app.get("/mapped", TestUtil.okHandler)
        assertThat(http.call(HttpMethod.HEAD, "/mapped").httpCode()).isEqualTo(OK)
    }

    @Test
    fun `urls are case sensitive`() = TestUtil.test { app, http ->
        app.get("/My-Url") { it.result("OK") }
        assertThat(http.get("/My-Url").httpCode()).isEqualTo(OK)
        assertThat(http.get("/MY-URL").httpCode()).isEqualTo(NOT_FOUND)
        assertThat(http.get("/my-url").httpCode()).isEqualTo(NOT_FOUND)
    }

    @Test
    fun `filers are executed in order`() = TestUtil.test { app, http ->
        app.before { it.result("1") }
        app.before { it.result(it.result() + "2") }
        app.get("/hello") { it.result(it.result() + "Hello") }
        app.after { it.result(it.result() + "3") }
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
        app.get("/hello:world") { it.result("Hello World") }
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
        assertThat(http.getBody("/test")).isEqualTo(NOT_FOUND.message)
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
        app.get("/{p1}") { it.result(it.pathParamMap().toString()) }
        assertThat(http.getBody("/param1")).isEqualTo("{p1=param1}")
    }

    @Test
    fun `can add multiple path params in same path segment`() = TestUtil.test { app, http ->
        app.get("/{p1}AND{p2}") { it.result(it.pathParamMap().toString()) }
        assertThat(http.getBody("/param1ANDparam2")).isEqualTo("{p1=param1, p2=param2}")
    }

    @Test
    fun `utf-8 encoded path-params work`() = TestUtil.test { app, http ->
        app.get("/{path-param}") { it.result(it.pathParam("path-param")) }
        assertThat(okHttp.getBody(http.origin + "/" + URLEncoder.encode("TE/ST", "UTF-8"))).isEqualTo("TE/ST")
    }

    @Test
    fun `path-params work case-sensitive`() = TestUtil.test { app, http ->
        app.get("/{userId}") { it.result(it.pathParam("userId")) }
        assertThat(http.getBody("/path-param")).isEqualTo("path-param")
        app.get("/{a}/{A}") { it.result("${it.pathParam("a")}-${it.pathParam("A")}") }
        assertThat(http.getBody("/a/B")).isEqualTo("a-B")
    }

    @Test
    fun `path-param values retain their casing`() = TestUtil.test { app, http ->
        app.get("/{path-param}") { it.result(it.pathParam("path-param")) }
        assertThat(http.getBody("/SomeCamelCasedValue")).isEqualTo("SomeCamelCasedValue")
    }

    @Test
    fun `path-params can be combined with regular content`() = TestUtil.test { app, http ->
        app.get("/hi-{name}") { it.result(it.pathParam("name")) }
        assertThat(http.getBody("/hi-world")).isEqualTo("world")
    }

    @Test
    fun `path-params can be combined with wildcards`() = TestUtil.test { app, http ->
        app.get("/hi-{name}-*") { it.result(it.pathParam("name")) }
        assertThat(http.get("/hi-world").httpCode()).isEqualTo(NOT_FOUND)
        val response = http.get("/hi-world-not-included")
        assertThat(response.httpCode()).isEqualTo(OK)
        assertThat(response.body).isEqualTo("world")
    }

    @Test
    fun `path-params support stars in names`() = TestUtil.test { app, http ->
        app.get("/hi-{name*}") { it.result(it.pathParam("name*")) }
        assertThat(http.getBody("/hi-world")).isEqualTo("world")
    }

    @Test
    fun `percentage operator does not consume text`() = TestUtil.test { app, http ->
        app.get("/{name}*") { it.result(it.pathParam("name")) }
        assertThat(http.getBody("/text")).isEqualTo("text")
        assertThat(http.getBody("/text/two")).isEqualTo("text")
    }

    @Test
    fun `path-params cannot directly follow a wildcard`() = TestUtil.test { app, _ ->
        assertThrows<WildcardBracketAdjacentException> {
            app.get("/*{name}") { it.result(it.pathParam("name")) }
        }
    }

    @Test
    fun `angle-bracket path-params can accept slashes`() = TestUtil.test { app, http ->
        app.get("/<name>") { it.result(it.pathParam("name")) }
        assertThat(http.getBody("/hi/with/slashes")).isEqualTo("hi/with/slashes")
    }

    @Test
    fun `angle-bracket path-params can be combined with regular content`() = TestUtil.test { app, http ->
        app.get("/hi/<name>") { it.result(it.pathParam("name")) }
        assertThat(http.getBody("/hi/with/slashes")).isEqualTo("with/slashes")
    }

    @Test
    fun `angle-bracket path-params can be combined with wildcards`() = TestUtil.test { app, http ->
        app.get("/hi-<name>-*") { it.result(it.pathParam("name")) }
        assertThat(http.get("/hi-world").httpCode()).isEqualTo(NOT_FOUND)
        val response = http.get("/hi-world/hi-not-included")
        assertThat(response.httpCode()).isEqualTo(OK)
        assertThat(response.body).isEqualTo("world/hi")
    }

    // looking for a solution to enable this on a per-path basis
    @Disabled
    @Test
    fun `path regex works`() = TestUtil.test { app, http ->
        app.get("/{path-param}/[0-9]+/") { it.result(it.pathParam("path-param")) }
        assertThat(http.get("/test/pathParam").httpCode()).isEqualTo(NOT_FOUND)
        assertThat(http.get("/test/21").body).isEqualTo("test")
    }

    @Test
    fun `automatic slash prefixing works`() = TestUtil.test { app, http ->
        app.routes {
            path("test") {
                path("{id}") {
                    get { it.result(it.pathParam("id")) }
                }
                get { it.result("test") }
            }
        }
        assertThat(http.getBody("/test/path-param/")).isEqualTo("path-param")
        assertThat(http.getBody("/test/")).isEqualTo("test")
    }

    @Test
    fun `non sub-path star wildcard works for plain paths`() = TestUtil.test { app, http ->
        app.get("/p") { it.result("1") }.also { assertThat(http.getBody("/p")).isEqualTo("1") }
        app.get("/p-test") { it.result("2") }.also { assertThat(http.getBody("/p-test")).isEqualTo("2") }
        app.after("/p*") { it.result("${it.result()}AFTER") }.also {
            assertThat(http.getBody("/p")).isEqualTo("1AFTER")
            assertThat(http.getBody("/p-test")).isEqualTo("2AFTER")
        }
    }

    @Test
    fun `non sub-path wildcard works for path-params`() = TestUtil.test { app, http ->
        app.get("/{pp}-test") { it.result("2") }.also { assertThat(http.getBody("/p-test")).isEqualTo("2") }
        app.get("/{pp}") { it.result("1") }.also { assertThat(http.getBody("/p")).isEqualTo("1") }
        app.after("/{pp}*") { it.result("${it.result()}AFTER") }.also {
            assertThat(http.getBody("/p")).isEqualTo("1AFTER")
            assertThat(http.getBody("/p-test")).isEqualTo("2AFTER")
        }
    }

    @Test
    fun `sub-path wildcard works for path-params`() = TestUtil.test { app, http ->
        app.routes {
            after("/partners/{pp}*") { it.result("${it.result()} - after") }
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
            app.get("/{param}/demo/<param>") { it.result(it.pathParam("param")) }
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
                app.get(it) { it.result("") }
            }
        }
    }

    @Test
    fun `root path works with ignoreTrailingSlashes set to false`() = TestUtil.test(Javalin.create {
        it.routing.ignoreTrailingSlashes = false
    }) { app, http ->
        app.get("/") { it.result("root") }
        app.get("/home") { it.result("home") }
        assertThat(http.getBody("/")).isEqualTo("root")
        assertThat(http.getBody("/home")).isEqualTo("home")
    }

    @Test
    fun `root path works with ApiBuilder and ignoreTrailingSlashes set to false`() = TestUtil.test(Javalin.create {
        it.routing.ignoreTrailingSlashes = false
    }) { app, http ->
        app.routes {
            get("/") { it.result("root") }
            get("/home") { it.result("home") }
        }
        assertThat(http.getBody("/")).isEqualTo("root")
        assertThat(http.getBody("/home")).isEqualTo("home")
    }
}
