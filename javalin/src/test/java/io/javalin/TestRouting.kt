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
import io.javalin.http.HandlerType
import io.javalin.http.HandlerType.TRACE
import io.javalin.http.HttpStatus
import io.javalin.http.HttpStatus.NOT_FOUND
import io.javalin.http.HttpStatus.METHOD_NOT_ALLOWED
import io.javalin.http.HttpStatus.OK
import io.javalin.http.NotFoundResponse
import io.javalin.plugin.bundled.RedirectToLowercasePathPlugin
import io.javalin.router.Endpoint
import io.javalin.router.EndpointMetadata
import io.javalin.router.EndpointNotFound
import io.javalin.router.matcher.MissingBracketsException
import io.javalin.router.matcher.ParameterNamesNotUniqueException
import io.javalin.router.matcher.WildcardBracketAdjacentException
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import io.javalin.websocket.WsHandlerType
import io.javalin.testing.HttpMethod
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
        assertThat(http.getStatus("/hello")).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/hello")).isEqualTo("Hello World")
    }

    @Test
    fun `routing is available in config`() = TestUtil.test(Javalin.create { cfg ->
        cfg.router.mount {
            it.get("/hello") { ctx -> ctx.result("Hello World") }
        }
    }) { _, http ->
        assertThat(http.getStatus("/hello")).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/hello")).isEqualTo("Hello World")
    }

    @Test
    fun `allHttpHandlers method works`() = TestUtil.test { app, http ->
        app.get("/1") { it.result("Hello World") }
        val handler1 = app.unsafeConfig().pvt.internalRouter.allHttpHandlers().map { it.endpoint }.find { it.path == "/1" }!!
        assertThat(handler1.path).isEqualTo("/1")
        assertThat(handler1.method).isEqualTo(HandlerType.GET)
        app.before("/2") { }
        val handler2 = app.unsafeConfig().pvt.internalRouter.allHttpHandlers().map { it.endpoint }.find { it.path == "/2" }!!
        assertThat(handler2.method).isEqualTo(HandlerType.BEFORE)
    }

    private object TestMetadata : EndpointMetadata

    @Test
    fun `custom metadata is available on endpoint`() {
        val app = Javalin.create { cfg ->
            cfg.router.mount { it ->
                it.addEndpoint(
                    Endpoint.create(HandlerType.GET, "/hello")
                        .addMetadata(TestMetadata)
                        .handler { ctx -> ctx.result("Hello World") }
                )
            }
        }

        val endpoint = app.unsafeConfig().pvt.internalRouter.allHttpHandlers().first { it.endpoint.path == "/hello" }.endpoint
        assertThat(endpoint.metadata(TestMetadata::class.java)).isEqualTo(TestMetadata)
    }

    @Test
    fun `allWsHandlers method works`() = TestUtil.test { app, http ->
        app.ws("/1") { }
        val handler1 = app.unsafeConfig().pvt.internalRouter.allWsHandlers().find { it.path == "/1" }!!
        assertThat(handler1.path).isEqualTo("/1")
        assertThat(handler1.type).isEqualTo(WsHandlerType.WEBSOCKET)
        app.wsBefore("/2") { }
        val handler2 = app.unsafeConfig().pvt.internalRouter.allWsHandlers().find { it.path == "/2" }!!
        assertThat(handler2.type).isEqualTo(WsHandlerType.WEBSOCKET_BEFORE)
    }

    @Test
    fun `api builder can be used as custom router`() = TestUtil.test(Javalin.create { cfg ->
        cfg.router.apiBuilder {
            get("/hello") { it.result("Hello World") }
        }
    }) { _, http ->
        assertThat(http.getStatus("/hello")).isEqualTo(HttpStatus.OK)
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
        app.addHttpHandler(TRACE, "/mapped", TestUtil.okHandler)

        for (httpMethod in HttpMethod.all()) {
            assertThat(http.call(httpMethod, "/mapped").httpCode()).isEqualTo(OK)
        }
    }

    @Test
    fun `all unmapped verbs return 404`() = TestUtil.test { _, http ->
        for (httpMethod in HttpMethod.all()) {
            val response = http.call(httpMethod, "/unmapped")
            assertThat(response.httpCode()).isEqualTo(NOT_FOUND)

            if (httpMethod != HttpMethod.HEAD) {
                assertThat(response.body).isEqualTo("Endpoint ${httpMethod.name()} /unmapped not found")
            }
        }
    }

    @Test
    fun `can handle endpoint not found differently than regular 404`() = TestUtil.test { app, http ->
        app.get("/user") { throw NotFoundResponse("User not found") }
        app.exception(EndpointNotFound::class.java) { _, ctx -> ctx.status(METHOD_NOT_ALLOWED) }
        val userNotFound = http.get("/user")
        assertThat(userNotFound.httpCode()).isEqualTo(NOT_FOUND)
        assertThat(userNotFound.body).isEqualTo("User not found")
        val endpointNotFound = http.get("/guild")
        assertThat(endpointNotFound.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
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
        assertThat(http.getStatus("/hello")).isEqualTo(HttpStatus.OK)
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
        assertThat(http.getStatus("/hello:world")).isEqualTo(HttpStatus.OK)
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
        assertThat(http.getBody("/test")).isEqualTo("Endpoint GET /test not found")
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
    fun `automatic slash prefixing works`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
                path("test") {
                    path("{id}") {
                        get { it.result(it.pathParam("id")) }
                    }
                    get { it.result("test") }
                }
            }
        }
    ) { app, http ->
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
    fun `sub-path wildcard works for path-params`() = TestUtil.test(
        Javalin.create {
            it.router.apiBuilder {
                after("/partners/{pp}*") { it.result("${it.result()} - after") }
                path("/partners/{pp}") {
                    get { it.result("root") }
                    get("/api") { it.result("api") }
                }
            }
        }
    ) { app, http ->
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
        it.router.ignoreTrailingSlashes = false
    }) { app, http ->
        app.get("/") { it.result("root") }
        app.get("/home") { it.result("home") }
        assertThat(http.getBody("/")).isEqualTo("root")
        assertThat(http.getBody("/home")).isEqualTo("home")
    }

    @Test
    fun `root path works with ApiBuilder and ignoreTrailingSlashes set to false`() = TestUtil.test(Javalin.create {
        it.router.apiBuilder {
            get("/") { it.result("root") }
            get("/home") { it.result("home") }
        }
        it.router.ignoreTrailingSlashes = false
    }) { app, http ->
        assertThat(http.getStatus("/")).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/")).isEqualTo("root")
        assertThat(http.getBody("/home")).isEqualTo("home")
    }

    @Test
    fun `case insensitive routes work with caseInsensitiveRoutes`() = TestUtil.test(Javalin.create {
        it.router.caseInsensitiveRoutes = true
    }) { app, http ->
        app.get("/paTh") { it.result("ok") }
        assertThat(http.getStatus("/path")).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/path")).isEqualTo("ok")
        assertThat(http.getBody("/PATH")).isEqualTo("ok")
        assertThat(http.getBody("/Path")).isEqualTo("ok")
        assertThat(http.getBody("/pAtH")).isEqualTo("ok")
    }

    @Test
    fun `case insensitive path params work with caseInsensitiveRoutes`() = TestUtil.test(Javalin.create {
        it.router.caseInsensitiveRoutes = true
    }) { app, http ->
        app.get("/patH/<param>") { it.result(it.pathParam("param")) }
        assertThat(http.getStatus("/path/value")).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/path/value")).isEqualTo("value")
        assertThat(http.getBody("/PATH/vAlUe")).isEqualTo("vAlUe")
        assertThat(http.getBody("/Path/VALUe")).isEqualTo("VALUe")
        assertThat(http.getBody("/pAtH/VALUE")).isEqualTo("VALUE")
    }

    @Test
    fun `enableRedirectToLowercasePaths with caseInsensitiveRoutes throws exception`() {
        assertThrows<java.lang.IllegalStateException> {
            Javalin.create {
                it.router.caseInsensitiveRoutes = true
                it.registerPlugin(RedirectToLowercasePathPlugin())
            }.start()
        }
    }

    @Test
    fun `invalid path results in 400`() = TestUtil.test { app, http ->
        app.get("/{path}") { it.result("Hello World") }
        assertThat(okHttp.getBody(http.origin + "/%+")).contains("Bad Request")
    }

}
