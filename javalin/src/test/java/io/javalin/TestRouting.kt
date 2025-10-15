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
import io.javalin.testing.*
import io.javalin.testing.httpCode
import io.javalin.websocket.WsHandlerType
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
        assertThat(http.getStatus("/hello")).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/hello")).isEqualTo("Hello World")
    }

    @Test
    fun `routing is available in config`() = TestUtil.test(Javalin.create { cfg ->
        cfg.routes.get("/hello") { ctx -> ctx.result("Hello World") }
    }) { _, http ->
        assertThat(http.getStatus("/hello")).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/hello")).isEqualTo("Hello World")
    }

    @Test
    fun `allHttpHandlers method works`() {
        val app = Javalin.create { config ->
            config.routes.get("/1") { it.result("Hello World") }
            config.routes.before("/2") { }
        }
        val handler1 = app.unsafeConfig().pvt.internalRouter.allHttpHandlers().map { it.endpoint }.find { it.path == "/1" }!!
        assertThat(handler1.path).isEqualTo("/1")
        assertThat(handler1.method).isEqualTo(HandlerType.GET)
        val handler2 = app.unsafeConfig().pvt.internalRouter.allHttpHandlers().map { it.endpoint }.find { it.path == "/2" }!!
        assertThat(handler2.method).isEqualTo(HandlerType.BEFORE)
    }

    private object TestMetadata : EndpointMetadata

    @Test
    fun `custom metadata is available on endpoint`() {
        val app = Javalin.create { cfg ->
            cfg.routes.addEndpoint(
                Endpoint.create(HandlerType.GET, "/hello")
                    .addMetadata(TestMetadata)
                    .handler { ctx -> ctx.result("Hello World") }
            )
        }

        val endpoint = app.unsafeConfig().pvt.internalRouter.allHttpHandlers().first { it.endpoint.path == "/hello" }.endpoint
        assertThat(endpoint.metadata(TestMetadata::class.java)).isEqualTo(TestMetadata)
    }

    @Test
    fun `allWsHandlers method works`() {
        val app = Javalin.create { config ->
            config.routes.ws("/1") { }
            config.routes.wsBefore("/2") { }
        }
        val handler1 = app.unsafeConfig().pvt.internalRouter.allWsHandlers().find { it.path == "/1" }!!
        assertThat(handler1.path).isEqualTo("/1")
        assertThat(handler1.type).isEqualTo(WsHandlerType.WEBSOCKET)
        val handler2 = app.unsafeConfig().pvt.internalRouter.allWsHandlers().find { it.path == "/2" }!!
        assertThat(handler2.type).isEqualTo(WsHandlerType.WEBSOCKET_BEFORE)
    }

    @Test
    fun `api builder can be used as custom router`() = TestUtil.test(Javalin.create { cfg ->
        cfg.routes.apiBuilder {
            get("/hello") { it.result("Hello World") }
        }
    }) { _, http ->
        assertThat(http.getStatus("/hello")).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/hello")).isEqualTo("Hello World")
    }

    @Test
    fun `all mapped verbs return 200`() {
        val app = Javalin.create { config ->
            for (httpMethod in HttpMethod.all()) {
                val handlerType = HandlerType.findOrCreate(httpMethod.name())
                config.routes.addEndpoint(Endpoint.create(handlerType, "/mapped").handler(TestUtil.okHandler))
            }
        }
        TestUtil.test(app) { _, http ->
            for (httpMethod in HttpMethod.all()) {
                assertThat(http.call(httpMethod, "/mapped").httpCode()).isEqualTo(OK)
            }
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
    fun `can handle endpoint not found differently than regular 404`() {
        val app = Javalin.create { config ->
            config.routes.get("/user") { throw NotFoundResponse("User not found") }
            config.routes.exception(EndpointNotFound::class.java) { _, ctx -> ctx.status(METHOD_NOT_ALLOWED) }
        }
        TestUtil.test(app) { _, http ->
            val userNotFound = http.get("/user")
            assertThat(userNotFound.httpCode()).isEqualTo(NOT_FOUND)
            assertThat(userNotFound.body).isEqualTo("User not found")
            val endpointNotFound = http.get("/guild")
            assertThat(endpointNotFound.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
        }
    }

    @Test
    fun `HEAD returns 200 if GET is mapped`() {
        val app = Javalin.create { config ->
            config.routes.get("/mapped", TestUtil.okHandler)
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.call(HttpMethod.HEAD, "/mapped").httpCode()).isEqualTo(OK)
        }
    }

    @Test
    fun `urls are case sensitive`() {
        val app = Javalin.create { config ->
            config.routes.get("/My-Url") { it.result("OK") }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.get("/My-Url").httpCode()).isEqualTo(OK)
            assertThat(http.get("/MY-URL").httpCode()).isEqualTo(NOT_FOUND)
            assertThat(http.get("/my-url").httpCode()).isEqualTo(NOT_FOUND)
        }
    }

    @Test
    fun `filers are executed in order`() {
        val app = Javalin.create { config ->
            config.routes.before { it.result("1") }
            config.routes.before { it.result(it.result() + "2") }
            config.routes.get("/hello") { it.result(it.result() + "Hello") }
            config.routes.after { it.result(it.result() + "3") }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getStatus("/hello")).isEqualTo(HttpStatus.OK)
            assertThat(http.getBody("/hello")).isEqualTo("12Hello3")
        }
    }

    @Test
    fun `old style colon path parameter throws exception`() {
        assertThatExceptionOfType(IllegalArgumentException::class.java)
            .isThrownBy {
                Javalin.create { config ->
                    config.routes.get("/:test") {}
                }
            }
            .withMessageStartingWith("Path '/:test' invalid - Javalin 4 switched from ':param' to '{param}'.")
    }

    @Test
    fun `literal colon in path segment works`() {
        val app = Javalin.create { config ->
            config.routes.get("/hello:world") { it.result("Hello World") }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getStatus("/hello:world")).isEqualTo(HttpStatus.OK)
            assertThat(http.getBody("/hello:world")).isEqualTo("Hello World")
        }
    }

    @Test
    fun `wildcard first works`() {
        val app = Javalin.create { config ->
            config.routes.get("/*/test") { it.result("!") }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/tast/test")).isEqualTo("!")
        }
    }

    @Test
    fun `wildcard last works`() {
        val app = Javalin.create { config ->
            config.routes.get("/test/*") { it.result("!") }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/test")).isEqualTo("Endpoint GET /test not found")
            assertThat(http.getBody("/test/1")).isEqualTo("!")
            assertThat(http.getBody("/test/tast")).isEqualTo("!")
        }
    }

    @Test
    fun `wildcard middle works`() {
        val app = Javalin.create { config ->
            config.routes.get("/test/*/test") { it.result("!") }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/test/en/test")).isEqualTo("!")
        }
    }

    @Test
    fun `path params work`() {
        val app = Javalin.create { config ->
            config.routes.get("/{p1}") { it.result(it.pathParamMap().toString()) }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/param1")).isEqualTo("{p1=param1}")
        }
    }

    @Test
    fun `can add multiple path params in same path segment`() {
        val app = Javalin.create { config ->
            config.routes.get("/{p1}AND{p2}") { it.result(it.pathParamMap().toString()) }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/param1ANDparam2")).isEqualTo("{p1=param1, p2=param2}")
        }
    }

    @Test
    fun `utf-8 encoded path-params work`() {
        val app = Javalin.create { config ->
            config.routes.get("/{path-param}") { it.result(it.pathParam("path-param")) }
        }
        TestUtil.test(app) { _, http ->
            assertThat(okHttp.getBody(http.origin + "/" + URLEncoder.encode("TE/ST", "UTF-8"))).isEqualTo("TE/ST")
        }
    }

    @Test
    fun `path-params work case-sensitive`() {
        val app = Javalin.create { config ->
            config.routes.get("/{userId}") { it.result(it.pathParam("userId")) }
            config.routes.get("/{a}/{A}") { it.result("${it.pathParam("a")}-${it.pathParam("A")}") }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/path-param")).isEqualTo("path-param")
            assertThat(http.getBody("/a/B")).isEqualTo("a-B")
        }
    }

    @Test
    fun `path-param values retain their casing`() {
        val app = Javalin.create { config ->
            config.routes.get("/{path-param}") { it.result(it.pathParam("path-param")) }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/SomeCamelCasedValue")).isEqualTo("SomeCamelCasedValue")
        }
    }

    @Test
    fun `path-params can be combined with regular content`() {
        val app = Javalin.create { config ->
            config.routes.get("/hi-{name}") { it.result(it.pathParam("name")) }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/hi-world")).isEqualTo("world")
        }
    }

    @Test
    fun `path-params can be combined with wildcards`() {
        val app = Javalin.create { config ->
            config.routes.get("/hi-{name}-*") { it.result(it.pathParam("name")) }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.get("/hi-world").httpCode()).isEqualTo(NOT_FOUND)
            val response = http.get("/hi-world-not-included")
            assertThat(response.httpCode()).isEqualTo(OK)
            assertThat(response.body).isEqualTo("world")
        }
    }

    @Test
    fun `path-params support stars in names`() {
        val app = Javalin.create { config ->
            config.routes.get("/hi-{name*}") { it.result(it.pathParam("name*")) }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/hi-world")).isEqualTo("world")
        }
    }

    @Test
    fun `percentage operator does not consume text`() {
        val app = Javalin.create { config ->
            config.routes.get("/{name}*") { it.result(it.pathParam("name")) }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/text")).isEqualTo("text")
            assertThat(http.getBody("/text/two")).isEqualTo("text")
        }
    }

    @Test
    fun `path-params cannot directly follow a wildcard`() {
        assertThrows<WildcardBracketAdjacentException> {
            Javalin.create { config ->
                config.routes.get("/*{name}") { it.result(it.pathParam("name")) }
            }
        }
    }

    @Test
    fun `angle-bracket path-params can accept slashes`() {
        val app = Javalin.create { config ->
            config.routes.get("/<name>") { it.result(it.pathParam("name")) }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/hi/with/slashes")).isEqualTo("hi/with/slashes")
        }
    }

    @Test
    fun `angle-bracket path-params can be combined with regular content`() {
        val app = Javalin.create { config ->
            config.routes.get("/hi/<name>") { it.result(it.pathParam("name")) }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/hi/with/slashes")).isEqualTo("with/slashes")
        }
    }

    @Test
    fun `angle-bracket path-params can be combined with wildcards`() {
        val app = Javalin.create { config ->
            config.routes.get("/hi-<name>-*") { it.result(it.pathParam("name")) }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.get("/hi-world").httpCode()).isEqualTo(NOT_FOUND)
            val response = http.get("/hi-world/hi-not-included")
            assertThat(response.httpCode()).isEqualTo(OK)
            assertThat(response.body).isEqualTo("world/hi")
        }
    }

    // looking for a solution to enable this on a per-path basis
    @Disabled
    @Test
    fun `path regex works`() {
        val app = Javalin.create { config ->
            config.routes.get("/{path-param}/[0-9]+/") { it.result(it.pathParam("path-param")) }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.get("/test/pathParam").httpCode()).isEqualTo(NOT_FOUND)
            assertThat(http.get("/test/21").body).isEqualTo("test")
        }
    }

    @Test
    fun `automatic slash prefixing works`() {
        val app = Javalin.create { config ->
            config.routes.apiBuilder {
                path("test") {
                    path("{id}") {
                        get { it.result(it.pathParam("id")) }
                    }
                    get { it.result("test") }
                }
            }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/test/path-param/")).isEqualTo("path-param")
            assertThat(http.getBody("/test/")).isEqualTo("test")
        }
    }

    @Test
    fun `non sub-path star wildcard works for plain paths`() {
        val app = Javalin.create { config ->
            config.routes.get("/p") { it.result("1") }
            config.routes.get("/p-test") { it.result("2") }
            config.routes.after("/p*") { it.result("${it.result()}AFTER") }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/p")).isEqualTo("1AFTER")
            assertThat(http.getBody("/p-test")).isEqualTo("2AFTER")
        }
    }

    @Test
    fun `non sub-path wildcard works for path-params`() {
        val app = Javalin.create { config ->
            config.routes.get("/{pp}-test") { it.result("2") }
            config.routes.get("/{pp}") { it.result("1") }
            config.routes.after("/{pp}*") { it.result("${it.result()}AFTER") }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/p")).isEqualTo("1AFTER")
            assertThat(http.getBody("/p-test")).isEqualTo("2AFTER")
        }
    }

    @Test
    fun `sub-path wildcard works for path-params`() {
        val app = Javalin.create { config ->
            config.routes.apiBuilder {
                after("/partners/{pp}*") { it.result("${it.result()} - after") }
                path("/partners/{pp}") {
                    get { it.result("root") }
                    get("/api") { it.result("api") }
                }
            }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/partners/microsoft")).isEqualTo("root - after")
            assertThat(http.getBody("/partners/microsoft/api")).isEqualTo("api - after")
        }
    }

    @Test
    fun `path param names are required to be unique across path param types`() {
        assertThatExceptionOfType(ParameterNamesNotUniqueException::class.java).isThrownBy {
            Javalin.create { config ->
                config.routes.get("/{param}/demo/<param>") { it.result(it.pathParam("param")) }
            }
        }
    }

    @Test
    fun `missing brackets lead to an exception`() {
        listOf(
            "/{",
            "/}",
            "/>",
            "/<",
            "/</>"
        ).forEach { path ->
            assertThatExceptionOfType(MissingBracketsException::class.java).describedAs(path).isThrownBy {
                Javalin.create { config ->
                    config.routes.get(path) { it.result("") }
                }
            }
        }
    }

    @Test
    fun `root path works with ignoreTrailingSlashes set to false`() {
        val app = Javalin.create { config ->
            config.router.ignoreTrailingSlashes = false
            config.routes.get("/") { it.result("root") }
            config.routes.get("/home") { it.result("home") }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getBody("/")).isEqualTo("root")
            assertThat(http.getBody("/home")).isEqualTo("home")
        }
    }

    @Test
    fun `root path works with ApiBuilder and ignoreTrailingSlashes set to false`() {
        val app = Javalin.create { config ->
            config.routes.apiBuilder {
                get("/") { it.result("root") }
                get("/home") { it.result("home") }
            }
            config.router.ignoreTrailingSlashes = false
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getStatus("/")).isEqualTo(HttpStatus.OK)
            assertThat(http.getBody("/")).isEqualTo("root")
            assertThat(http.getBody("/home")).isEqualTo("home")
        }
    }

    @Test
    fun `case insensitive routes work with caseInsensitiveRoutes`() {
        val app = Javalin.create { config ->
            config.router.caseInsensitiveRoutes = true
            config.routes.get("/paTh") { it.result("ok") }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getStatus("/path")).isEqualTo(HttpStatus.OK)
            assertThat(http.getBody("/path")).isEqualTo("ok")
            assertThat(http.getBody("/PATH")).isEqualTo("ok")
            assertThat(http.getBody("/Path")).isEqualTo("ok")
            assertThat(http.getBody("/pAtH")).isEqualTo("ok")
        }
    }

    @Test
    fun `case insensitive path params work with caseInsensitiveRoutes`() {
        val app = Javalin.create { config ->
            config.router.caseInsensitiveRoutes = true
            config.routes.get("/patH/<param>") { it.result(it.pathParam("param")) }
        }
        TestUtil.test(app) { _, http ->
            assertThat(http.getStatus("/path/value")).isEqualTo(HttpStatus.OK)
            assertThat(http.getBody("/path/value")).isEqualTo("value")
            assertThat(http.getBody("/PATH/vAlUe")).isEqualTo("vAlUe")
            assertThat(http.getBody("/Path/VALUe")).isEqualTo("VALUe")
            assertThat(http.getBody("/pAtH/VALUE")).isEqualTo("VALUE")
        }
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
    fun `invalid path results in 400`() {
        val app = Javalin.create { config ->
            config.routes.get("/{path}") { it.result("Hello World") }
        }
        TestUtil.test(app) { _, http ->
            assertThat(okHttp.getBody(http.origin + "/%+")).contains("Bad Request")
        }
    }

}
