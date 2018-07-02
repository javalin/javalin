/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.core.util.Header
import io.javalin.util.BaseTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsString
import org.junit.Test

class TestRequest : BaseTest() {

    /*
     * Session
     */
    @Test
    fun test_session_works() {
        app.get("/store-session") { ctx -> ctx.req.session.setAttribute("test", "tast") }
        app.get("/read-session") { ctx -> ctx.result(ctx.req.session.getAttribute("test") as String) }
        http.getBody_withCookies("/store-session")
        assertThat(http.getBody_withCookies("/read-session"), `is`("tast"))
    }

    @Test
    fun test_session_isHttpOnly() {
        app.get("/store-session") { ctx -> ctx.sessionAttribute("test", "tast") }
        assertThat(http.get_withCookies("/store-session").headers.getFirst("Set-Cookie").contains("HttpOnly"), `is`(true))
    }

    @Test
    fun test_sessionShorthand_works() {
        app.get("/store-session") { ctx -> ctx.sessionAttribute("test", "tast") }
        app.get("/read-session") { ctx -> ctx.result(ctx.sessionAttribute<Any>("test") as String) }
        http.getBody_withCookies("/store-session")
        assertThat(http.getBody_withCookies("/read-session"), `is`("tast"))
    }

    @Test
    fun test_sessionAttributeMap_works() {
        app.get("/store-session") { ctx ->
            ctx.sessionAttribute("test", "tast")
            ctx.sessionAttribute("hest", "hast")
        }
        app.get("/read-session") { ctx -> ctx.result(ctx.sessionAttributeMap<Any>().toString()) }
        http.getBody_withCookies("/store-session")
        assertThat(http.getBody_withCookies("/read-session"), `is`("{test=tast, hest=hast}"))
    }

    @Test
    fun test_attributesCanBeNull() {
        app.get("/store") { ctx ->
            ctx.attribute("test", "not-null")
            ctx.attribute("test", null)
            ctx.sessionAttribute("tast", "not-null")
            ctx.sessionAttribute("tast", null)
        }
        app.get("/read") { ctx -> ctx.result("${ctx.sessionAttribute<Any?>("tast")} and ${ctx.attribute<Any?>("test")}") }
        http.getBody_withCookies("/store")
        assertThat(http.getBody("/read"), `is`("null and null"))
    }

    /*
     * Cookies
     */
    @Test
    fun test_getSingleCookie_worksForMissingCookie() {
        app.get("/read-cookie") { ctx -> ctx.result("" + ctx.cookie("my-cookie")) }
        assertThat(http.getBody_withCookies("/read-cookie"), `is`("null"))
    }

    @Test
    fun test_getSingleCookie_worksForCookie() {
        app.get("/read-cookie") { ctx -> ctx.result(ctx.cookie("my-cookie")!!) }
        val response = Unirest.get("$origin/read-cookie").header(Header.COOKIE, "my-cookie=my-cookie-value").asString()
        assertThat(response.body, `is`("my-cookie-value"))
    }

    @Test
    fun test_getMultipleCookies_worksForNoCookies() {
        app.get("/read-cookie") { ctx -> ctx.result(ctx.cookieMap().toString()) }
        assertThat(http.getBody("/read-cookie"), `is`("{}"))
    }

    @Test
    fun test_getMultipleCookies_worksForMultipleCookies() {
        app.get("/read-cookie") { ctx -> ctx.result(ctx.cookieMap().toString()) }
        val response = Unirest.get("$origin/read-cookie").header(Header.COOKIE, "k1=v1;k2=v2;k3=v3").asString()
        assertThat(response.body, containsString("k1=v1, k2=v2, k3=v3"))
    }

    /*
     * Path params
     */
    @Test
    fun test_pathParamWorks_invalidParam() {
        app.get("/:my/:path") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        assertThat(http.getBody("/my/path"), `is`("Internal server error"))
    }

    @Test
    fun test_pathParamWorks_multipleSingleParams() {
        app.get("/:1/:2/:3") { ctx -> ctx.result(ctx.pathParam("1") + ctx.pathParam("2") + ctx.pathParam("3")) }
        assertThat(http.getBody("/my/path/params"), `is`("mypathparams"))
    }

    @Test
    fun test_pathParamMapWorks_noParamsPresent() {
        app.get("/my/path/params") { ctx -> ctx.result(ctx.pathParamMap().toString()) }
        assertThat(http.getBody("/my/path/params"), `is`("{}"))
    }

    @Test
    fun test_pathParamMapWorks_paramsPresent() {
        app.get("/:1/:2/:3") { ctx -> ctx.result(ctx.pathParamMap().toString()) }
        assertThat(http.getBody("/my/path/params"), `is`("{1=my, 2=path, 3=params}"))
    }

    /*
     * Query params
     */
    @Test
    fun test_queryParamWorks_noParam() {
        app.get("/") { ctx -> ctx.result("" + ctx.queryParam("qp")) }
        assertThat(http.getBody("/"), `is`("null")) // notice {"" + req} on previous line
    }

    @Test
    fun test_queryParamWorks_noParamButDefault() {
        app.get("/") { ctx -> ctx.result("" + ctx.queryParam("qp", "default")!!) }
        assertThat(http.getBody("/"), `is`("default"))
    }

    @Test
    fun test_queryParamWorks_multipleSingleParams() {
        app.get("/") { ctx -> ctx.result(ctx.queryParam("qp1") + ctx.queryParam("qp2") + ctx.queryParam("qp3")) }
        assertThat(http.getBody("/?qp1=1&qp2=2&qp3=3"), `is`("123"))
    }

    @Test
    fun test_queryParamsWorks_noParamsPresent() {
        app.get("/") { ctx ->
            val params = ctx.queryParams("qp1")
            ctx.result(params.toString())
        }
        assertThat(http.getBody("/"), `is`("[]"))
    }

    @Test
    fun test_queryParamsWorks_paramsPresent() {
        app.get("/") { ctx ->
            val params = ctx.queryParams("qp1")
            ctx.result(params.toString())
        }
        assertThat(http.getBody("/?qp1=1&qp1=2&qp1=3"), `is`("[1, 2, 3]"))
    }

    @Test
    fun test_anyQueryParamNullTrue_allParamsNull() {
        app.get("/") { ctx -> ctx.result("" + ctx.anyQueryParamNull("nullkey", "othernullkey")) }
        assertThat(http.getBody("/"), `is`("true"))
    }

    @Test
    fun test_anyQueryParamNullTrue_someParamsNull() {
        app.get("/") { ctx -> ctx.result("" + ctx.anyQueryParamNull("qp1", "qp2", "nullkey")) }
        assertThat(http.getBody("/?qp1=1&qp2=2"), `is`("true"))
    }

    @Test
    fun test_anyQueryParamNullFalse_allParamsNonNull() {
        app.get("/") { ctx -> ctx.result("" + ctx.anyQueryParamNull("qp1", "qp2", "qp3")) }
        assertThat(http.getBody("/?qp1=1&qp2=2&qp3=3"), `is`("false"))
    }

    /*
     * Form params
     */
    @Test
    fun test_formParamWorks() {
        app.post("/") { ctx -> ctx.result("" + ctx.formParam("fp1")!!) }
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().body, `is`("1"))
    }

    @Test
    fun test_formParamWorks_noParam() {
        app.post("/") { ctx -> ctx.result("" + ctx.formParam("fp3")) }
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().body, `is`("null"))
    }

    @Test
    fun test_formParamWorks_noParamButDefault() {
        app.post("/") { ctx -> ctx.result("" + ctx.formParam("fp4", "4")!!) }
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().body, `is`("4"))
    }

    @Test
    fun test_anyFormParamNullTrue_someParamsNull() {
        app.post("/") { ctx -> ctx.result("" + ctx.anyFormParamNull("fp1", "fp2", "nullkey")) }
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().body, `is`("true"))
    }

    @Test
    fun test_anyFormParamNullFalse_allParamsNonNull() {
        app.post("/") { ctx -> ctx.result("" + ctx.anyFormParamNull("fp1", "fp2", "fp3")) }
        assertThat(http.post("/").body("fp1=1&fp2=2&fp3=3").asString().body, `is`("false"))
    }

    @Test
    fun test_basicAuth_works() {
        app.get("/") { ctx ->
            val basicAuthCredentials = ctx.basicAuthCredentials()
            ctx.result(basicAuthCredentials!!.username + "|" + basicAuthCredentials.password)
        }
        val response = Unirest.get("$origin/").basicAuth("some-username", "some-password").asString()
        assertThat(response.body, `is`("some-username|some-password"))
    }

    @Test
    fun test_matchingPaths_works() {
        app.get("/matched") { ctx -> ctx.result(ctx.matchedPath()) }
        app.get("/matched/:path-param") { ctx -> ctx.result(ctx.matchedPath()) }
        app.after("/matched/:path-param/:param2") { ctx -> ctx.result(ctx.matchedPath()) }
        assertThat(http.getBody("/matched"), `is`("/matched"))
        assertThat(http.getBody("/matched/p1"), `is`("/matched/:path-param"))
        assertThat(http.getBody("/matched/p1/p2"), `is`("/matched/:path-param/:param2"))
    }

    @Test
    fun test_servletContext_isNotNull() {
        app.get("/") { ctx -> ctx.result(if (ctx.req.servletContext != null) "not-null" else "null") }
        assertThat(http.getBody("/"), `is`("not-null"))
    }

    /**
     * Param mapping
     */

    @Test
    fun test_mapQueryParams_worksForGoodInput() {
        app.get("/") { ctx ->
            val (name, email, phone) = ctx.mapQueryParams("name", "email", "phone") ?: throw IllegalArgumentException()
            ctx.result("$name|$email|$phone")
        }
        assertThat(http.getBody("/?name=some%20name&email=some%20email&phone=some%20phone"), `is`("some name|some email|some phone"))
    }

    @Test
    fun test_mapQueryParams_isNullForBadInput() {
        app.get("/") { ctx ->
            val (name, missing) = ctx.mapQueryParams("name", "missing") ?: throw IllegalArgumentException()
            ctx.result("$name|$missing")
        }
        assertThat(http.getBody("/?name=some%20name"), `is`("Internal server error"))
    }

    @Test
    fun test_mapFormParams_worksForGoodInput() {
        app.post("/") { ctx ->
            val (name, email, phone) = ctx.mapFormParams("name", "email", "phone") ?: throw IllegalArgumentException()
            ctx.result("$name|$email|$phone")
        }
        assertThat(http.post("/").body("name=some%20name&email=some%20email&phone=some%20phone").asString().body, `is`("some name|some email|some phone"))
    }

    @Test
    fun test_mapFormParams_isNullForBadInput() {
        app.post("/") { ctx ->
            val (name, missing) = ctx.mapFormParams("missing") ?: throw IllegalArgumentException()
            ctx.result("$name|$missing")
        }
        assertThat(http.post("/").body("name=some%20name").asString().body, `is`("Internal server error"))
    }

}
