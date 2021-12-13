/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.core.security.BasicAuthFilter
import io.javalin.core.util.Header
import io.javalin.http.staticfiles.Location
import io.javalin.http.util.ContextUtil
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestRequest {

    /*
     * Session/Attributes
     */
    @Test
    fun `session-attributes work`() = TestUtil.test { app, http ->
        app.get("/store-session") { ctx -> ctx.req.session.setAttribute("test", "tast") }
        app.get("/read-session") { ctx -> ctx.result(ctx.req.session.getAttribute("test") as String) }
        http.getBody("/store-session")
        assertThat(http.getBody("/read-session")).isEqualTo("tast")
    }

    @Test
    fun `session-cookie is http-only`() = TestUtil.test { app, http ->
        app.get("/store-session") { ctx -> ctx.sessionAttribute("test", "tast") }
        assertThat(http.get("/store-session").headers.getFirst("Set-Cookie").contains("HttpOnly")).isTrue()
    }

    @Test
    fun `session-attribute shorthand work`() = TestUtil.test { app, http ->
        app.get("/store-session") { ctx -> ctx.sessionAttribute("test", "tast") }
        app.get("/read-session") { ctx -> ctx.result(ctx.sessionAttribute<String>("test")!!) }
        http.getBody("/store-session")
        assertThat(http.getBody("/read-session")).isEqualTo("tast")
    }

    @Test
    fun `session-attribute can be consumed easily`() = TestUtil.test { app, http ->
        app.get("/store-attr") { it.sessionAttribute("attr", "Rowin") }
        app.get("/read-attr") { it.result(it.consumeSessionAttribute("attr") ?: "Consumed") }
        http.getBody("/store-attr") // store the attribute
        assertThat(http.getBody("/read-attr")).isEqualTo("Rowin") // read (and consume) the attribute
        assertThat(http.getBody("/read-attr")).isEqualTo("Consumed") // fallback
    }

    @Test
    fun `session-attribute-map works`() = TestUtil.test { app, http ->
        app.get("/store-session") { ctx ->
            ctx.sessionAttribute("test", "tast")
            ctx.sessionAttribute("hest", "hast")
        }
        app.get("/read-session") { ctx -> ctx.result(ctx.sessionAttributeMap().toString()) }
        http.getBody("/store-session")
        assertThat(http.getBody("/read-session")).isEqualTo("{test=tast, hest=hast}")
    }

    @Test
    fun `cached session attributes are cached when set`() = TestUtil.test { app, http ->
        app.get("/cached-session-attr") { ctx ->
            ctx.cachedSessionAttribute("test", "tast")
            ctx.result(ctx.attribute<String>("${ContextUtil.sessionCacheKeyPrefix}test")!!) // should be cached as a normal attribute
        }
        assertThat(http.getBody("/cached-session-attr")).contains("tast")
    }

    @Test
    fun `cached session attributes are cached when read`() = TestUtil.test { app, http ->
        app.get("/set-cached-session-attr") { it.cachedSessionAttribute("test", "tast") }
        app.get("/get-cached-session-attr") {
            it.cachedSessionAttribute<String>("test")
            it.result(it.attributeMap().toString())
        }
        app.get("/attr-map") { it.result(it.attributeMap().toString()) }
        http.getBody("/set-cached-session-attr") // first we set the cached session variable
        assertThat(http.getBody("/attr-map")).doesNotContain("test=tast") // we inspect the "cache", our key/value pair should not be here
        assertThat(http.getBody("/get-cached-session-attr")).contains("${ContextUtil.sessionCacheKeyPrefix}test=tast") // since we've accessed the variable, cache should now contain key/value pair
    }

    @Test
    fun `attributes can be removed`() = TestUtil.test { app, http ->
        app.get("/store") { ctx ->
            ctx.attribute("test", "not-null")
            ctx.attribute("test", null)
            ctx.sessionAttribute("tast", "not-null")
            ctx.sessionAttribute("tast", null)
        }
        app.get("/read") { ctx -> ctx.result("${ctx.sessionAttribute<Any?>("tast")} and ${ctx.attribute<Any?>("test")}") }
        http.getBody("/store")
        assertThat(http.getBody("/read")).isEqualTo("null and null")
    }

    @Test
    fun `attributeMap works`() = TestUtil.test { app, http ->
        app.get("/attr-map") { ctx ->
            ctx.attribute("test", "tast")
            ctx.attribute("hest", "hast")
            ctx.result(ctx.attributeMap().toString())
        }
        assertThat(http.getBody("/attr-map")).contains("test=tast")
        assertThat(http.getBody("/attr-map")).contains("hest=hast")
    }

    /*
     * Path params
     */
    @Test
    fun `pathParam throws for invalid param`() = TestUtil.test { app, http ->
        app.get("/{my}/{path}") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        assertThat(http.getBody("/my/path")).isEqualTo("Internal server error")
    }

    @Test
    fun `pathParam works for multiple params`() = TestUtil.test { app, http ->
        app.get("/{1}/{2}/{3}") { ctx -> ctx.result(ctx.pathParam("1") + ctx.pathParam("2") + ctx.pathParam("3")) }
        assertThat(http.getBody("/my/path/params")).isEqualTo("mypathparams")
    }

    @Test
    fun `pathParamMap returns empty map if no path params present`() = TestUtil.test { app, http ->
        app.get("/my/path/params") { ctx -> ctx.result(ctx.pathParamMap().toString()) }
        assertThat(http.getBody("/my/path/params")).isEqualTo("{}")
    }

    @Test
    fun `pathParamMap returns all present path-params`() = TestUtil.test { app, http ->
        app.get("/{1}/{2}/{3}") { ctx -> ctx.result(ctx.pathParamMap().toString()) }
        assertThat(http.getBody("/my/path/params")).isEqualTo("{1=my, 2=path, 3=params}")
    }

    /*
     * Query params
     */
    @Test
    fun `queryParam returns null for unknown param`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result("" + ctx.queryParam("qp")) }
        assertThat(http.getBody("/")).isEqualTo("null")
    }

    @Test
    fun `queryParam defaults to default value`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result("" + ctx.queryParamAsClass<String>("qp").getOrDefault("default")) }
        assertThat(http.getBody("/")).isEqualTo("default")
    }

    @Test
    fun `queryParam returns supplied values`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.queryParam("qp1") + ctx.queryParam("qp2") + ctx.queryParam("qp3")) }
        assertThat(http.getBody("/?qp1=1&qp2=2&qp3=3")).isEqualTo("123")
    }

    @Test
    fun `queryParam returns value containing equal sign`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.queryParam("equation")!!) }
        assertThat(http.getBody("/?equation=2*2=4")).isEqualTo("2*2=4")
    }

    @Test
    fun `queryParams returns empty list for unknown param`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.queryParams("qp1").toString()) }
        assertThat(http.getBody("/")).isEqualTo("[]")
    }

    @Test
    fun `queryParams returns list of supplied params`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.queryParams("qp1").toString()) }
        assertThat(http.getBody("/?qp1=1&qp1=2&qp1=3")).isEqualTo("[1, 2, 3]")
    }

    /*
     * Form params
     */
    @Test
    fun `formParam returns supplied form-param`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result("" + ctx.formParam("fp1")!!) }
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().body).isEqualTo("1")
    }

    @Test
    fun `formParam returns null for unknown param`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result("" + ctx.formParam("fp3")) }
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().body).isEqualTo("null")
    }

    @Test
    fun `formParam returns defaults to default value`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result("" + ctx.formParamAsClass<Int>("fp4").getOrDefault(4)) }
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().body).isEqualTo("4")
    }

    @Test
    fun `hasBasicAuthCredentials with Authorization header`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val basicAuthCredentialsExist = ctx.basicAuthCredentialsExist()
            ctx.result(basicAuthCredentialsExist.toString())
        }
        val response = Unirest.get("${http.origin}/").basicAuth("some-username", "some-pass:::word").asString()
        assertThat(response.body).isEqualTo("true")
    }

    @Test
    fun `hasBasicAuthCredentials without Authorization header`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val basicAuthCredentialsExist = ctx.basicAuthCredentialsExist()
            ctx.result(basicAuthCredentialsExist.toString())
        }
        val response = Unirest.get("${http.origin}/").asString()
        assertThat(response.body).isEqualTo("false")
    }

    @Test
    fun `basicAuthCredentials extracts username and password`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val basicAuthCredentials = ctx.basicAuthCredentials()
            ctx.result(basicAuthCredentials.username + "|" + basicAuthCredentials.password)
        }
        val response = Unirest.get("${http.origin}/").basicAuth("some-username", "some-password").asString()
        assertThat(response.body).isEqualTo("some-username|some-password")
    }

    @Test
    fun `basicAuthCredentials extracts username and password with colon`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val basicAuthCredentials = ctx.basicAuthCredentials()
            ctx.result(basicAuthCredentials.username + "|" + basicAuthCredentials.password)
        }
        val response = Unirest.get("${http.origin}/").basicAuth("some-username", "some-pass:::word").asString()
        assertThat(response.body).isEqualTo("some-username|some-pass:::word")
    }

    @Test
    fun `basicauth requires Basic prefix to header`() = TestUtil.test { app, http ->
        app.get("/") {
            try {
                it.basicAuthCredentials()
            } catch (e: IllegalArgumentException) {
                it.result(e.message!!)
            }
        }
        val response = Unirest.get("${http.origin}/").header(Header.AUTHORIZATION, "user:pass").asString()
        assertThat(response.body).isEqualTo("Invalid basicauth header. Value was 'user:pass'.")
    }

    @Test
    fun `basic auth filter plugin works`() {
        val basicauthApp = Javalin.create {
            it.registerPlugin(BasicAuthFilter("u", "p"))
            it.addStaticFiles("/public", Location.CLASSPATH)
        }.get("/hellopath") { it.result("Hello") }
        TestUtil.test(basicauthApp) { _, http ->
            assertThat(http.getBody("/hellopath")).isEqualTo("Unauthorized")
            assertThat(http.getBody("/html.html")).contains("Unauthorized")
            Unirest.get("${http.origin}/hellopath").basicAuth("u", "p").asString().let { assertThat(it.body).isEqualTo("Hello") }
            Unirest.get("${http.origin}/html.html").basicAuth("u", "p").asString().let { assertThat(it.body).contains("HTML works") }
        }
    }

    @Test
    fun `matchedPath returns the path used to match the request`() = TestUtil.test { app, http ->
        app.get("/matched") { ctx -> ctx.result(ctx.matchedPath()) }
        app.get("/matched/{path-param}") { ctx -> ctx.result(ctx.matchedPath()) }
        app.after("/matched/{path-param}/{param2}") { ctx -> ctx.result(ctx.matchedPath()) }
        assertThat(http.getBody("/matched")).isEqualTo("/matched")
        assertThat(http.getBody("/matched/p1")).isEqualTo("/matched/{path-param}")
        assertThat(http.getBody("/matched/p1/p2")).isEqualTo("/matched/{path-param}/{param2}")
    }

    @Test
    fun `endpointHandlerPath returns the path used to match the request, excluding any AFTER handlers`() = TestUtil.test { app, http ->
        app.before { }
        app.get("/matched/{path-param}") { }
        app.get("/matched/{another-path-param}") { }
        app.after { ctx -> ctx.result(ctx.endpointHandlerPath()) }
        assertThat(http.getBody("/matched/p1")).isEqualTo("/matched/{path-param}")
    }

    @Test
    fun `endpointHandlerPath doesn't crash for 404s`() = TestUtil.test { app, http ->
        app.before { }
        app.after { it.result(it.endpointHandlerPath()) }
        assertThat(http.getBody("/")).isEqualTo("No handler matched request path/method (404/405)")
    }

    @Test
    fun `servlet-context is not null`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(if (ctx.req.servletContext != null) "not-null" else "null") }
        assertThat(http.getBody("/")).isEqualTo("not-null")
    }

    /**
     * Simple proxy methods
     */
    @Test
    fun `contentLength works`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result(ctx.contentLength().toString()) }
        assertThat(http.post("/").body("Hello").asString().body).isEqualTo("5")
    }

    @Test
    fun `host works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.host()!!) }
        assertThat(http.getBody("/")).isEqualTo("localhost:" + app.port())
    }

    @Test
    fun `ip works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.ip()) }
        assertThat(http.getBody("/")).isEqualTo("127.0.0.1")
    }

    @Test
    fun `protocol works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.protocol()) }
        assertThat(http.getBody("/")).isEqualTo("HTTP/1.1")
    }

    @Test
    fun `scheme works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.scheme()) }
        assertThat(http.getBody("/")).isEqualTo("http")
    }

    @Test
    fun `url works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.url()) }
        assertThat(http.getBody("/")).isEqualTo("http://localhost:" + app.port() + "/")
    }

    @Test
    fun `fullUrl works`() = TestUtil.test { app, http ->
        val origin = "http://localhost:" + app.port() + "/";
        app.get("/") { ctx -> ctx.result(ctx.fullUrl()) }
        assertThat(http.getBody("/")).isEqualTo(origin)
        assertThat(http.getBody("/?test")).isEqualTo(origin + "?test")
        assertThat(http.getBody("/?test=tast")).isEqualTo(origin + "?test=tast")
    }

    @Test
    fun `empty contextPath works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.contextPath()) }
        assertThat(http.getBody("/")).isEqualTo("")
    }

    @Test
    fun `contextPath with value works`() = TestUtil.test(Javalin.create { it.contextPath = "/ctx" }) { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.contextPath()) }
        assertThat(http.getBody("/ctx/")).isEqualTo("/ctx")
    }

    @Test
    fun `userAgent works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.userAgent()!!) }
        assertThat(http.getBody("/")).isEqualTo("unirest-java/1.3.11")
    }

    @Test
    fun `validator header works`() = TestUtil.test { app, http ->
        app.get("/") { it.result(it.headerAsClass<Double>("double-header").get().javaClass.name) }
        assertThat(http.getBody("/", mapOf("double-header" to "12.34"))).isEqualTo("java.lang.Double")
    }
}
