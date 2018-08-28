/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.core.util.Header
import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsString
import org.junit.Test

class TestRequest {

    /*
     * Session/Attributes
     */
    @Test
    fun `session-attributes work`() = TestUtil.test { app, http ->
        app.get("/store-session") { ctx -> ctx.req.session.setAttribute("test", "tast") }
        app.get("/read-session") { ctx -> ctx.result(ctx.req.session.getAttribute("test") as String) }
        http.getBody("/store-session")
        assertThat(http.getBody("/read-session"), `is`("tast"))
    }

    @Test
    fun `session-cookie is http-only`() = TestUtil.test { app, http ->
        app.get("/store-session") { ctx -> ctx.sessionAttribute("test", "tast") }
        assertThat(http.get("/store-session").headers.getFirst("Set-Cookie").contains("HttpOnly"), `is`(true))
    }

    @Test
    fun `session-attribute shorthand work`() = TestUtil.test { app, http ->
        app.get("/store-session") { ctx -> ctx.sessionAttribute("test", "tast") }
        app.get("/read-session") { ctx -> ctx.result(ctx.sessionAttribute<String>("test")!!) }
        http.getBody("/store-session")
        assertThat(http.getBody("/read-session"), `is`("tast"))
    }

    @Test
    fun `session-attribute-map works`() = TestUtil.test { app, http ->
        app.get("/store-session") { ctx ->
            ctx.sessionAttribute("test", "tast")
            ctx.sessionAttribute("hest", "hast")
        }
        app.get("/read-session") { ctx -> ctx.result(ctx.sessionAttributeMap<Any>().toString()) }
        http.getBody("/store-session")
        assertThat(http.getBody("/read-session"), `is`("{test=tast, hest=hast}"))
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
        assertThat(http.getBody("/read"), `is`("null and null"))
    }

    @Test
    fun `attributeMap works`() = TestUtil.test { app, http ->
        app.get("/attr-map") { ctx ->
            ctx.attribute("test", "tast")
            ctx.attribute("hest", "hast")
            ctx.result(ctx.attributeMap<Any>().toString())
        }
        assertThat(http.getBody("/attr-map"), containsString("test=tast"))
        assertThat(http.getBody("/attr-map"), containsString("hest=hast"))
    }

    /*
     * Cookies
     */
    @Test
    fun `single cookie returns null when missing`() = TestUtil.test { app, http ->
        app.get("/read-cookie-1") { ctx -> ctx.result("" + ctx.cookie("my-cookie")) }
        assertThat(http.getBody("/read-cookie-1"), `is`("null"))
    }

    @Test
    fun `single cookie works`() = TestUtil.test { app, http ->
        app.get("/read-cookie-2") { ctx -> ctx.result(ctx.cookie("my-cookie")!!) }
        val response = Unirest.get("${http.origin}/read-cookie-2").header(Header.COOKIE, "my-cookie=my-cookie-value").asString()
        assertThat(response.body, `is`("my-cookie-value"))
    }

    @Test
    fun `cookie-map returns empty when no cookies are set`() = TestUtil.test { app, http ->
        app.get("/read-cookie-3") { ctx -> ctx.result(ctx.cookieMap().toString()) }
        assertThat(http.getBody("/read-cookie-3"), `is`("{}"))
    }

    @Test
    fun `cookie-map returns all cookies if cookies are set`() = TestUtil.test { app, http ->
        app.get("/read-cookie-4") { ctx -> ctx.result(ctx.cookieMap().toString()) }
        val response = Unirest.get("${http.origin}/read-cookie-4").header(Header.COOKIE, "k1=v1;k2=v2;k3=v3").asString()
        assertThat(response.body, `is`("{k1=v1, k2=v2, k3=v3}"))
    }

    /*
     * Path params
     */
    @Test
    fun `pathParam() throws for invalid param`() = TestUtil.test { app, http ->
        app.get("/:my/:path") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        assertThat(http.getBody("/my/path"), `is`("Internal server error"))
    }

    @Test
    fun `pathParam() works for multiple params`() = TestUtil.test { app, http ->
        app.get("/:1/:2/:3") { ctx -> ctx.result(ctx.pathParam("1") + ctx.pathParam("2") + ctx.pathParam("3")) }
        assertThat(http.getBody("/my/path/params"), `is`("mypathparams"))
    }

    @Test
    fun `pathParamMap() returns empty map if no path params present`() = TestUtil.test { app, http ->
        app.get("/my/path/params") { ctx -> ctx.result(ctx.pathParamMap().toString()) }
        assertThat(http.getBody("/my/path/params"), `is`("{}"))
    }

    @Test
    fun `pathParamMap() returns all present path-params`() = TestUtil.test { app, http ->
        app.get("/:1/:2/:3") { ctx -> ctx.result(ctx.pathParamMap().toString()) }
        assertThat(http.getBody("/my/path/params"), `is`("{1=my, 2=path, 3=params}"))
    }

    /*
     * Query params
     */
    @Test
    fun `queryParam() returns null for unknown param`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result("" + ctx.queryParam("qp")) }
        assertThat(http.getBody("/"), `is`("null"))
    }

    @Test
    fun `queryParam() defaults to default value`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result("" + ctx.queryParam("qp", "default")!!) }
        assertThat(http.getBody("/"), `is`("default"))
    }

    @Test
    fun `queryParam() returns supplied values`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.queryParam("qp1") + ctx.queryParam("qp2") + ctx.queryParam("qp3")) }
        assertThat(http.getBody("/?qp1=1&qp2=2&qp3=3"), `is`("123"))
    }

    @Test
    fun `queryParams() returns empty list for unknown param`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.queryParams("qp1").toString()) }
        assertThat(http.getBody("/"), `is`("[]"))
    }

    @Test
    fun `queryParams() returns list of supplied params`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.queryParams("qp1").toString()) }
        assertThat(http.getBody("/?qp1=1&qp1=2&qp1=3"), `is`("[1, 2, 3]"))
    }

    @Test
    fun `anyQueryParamNull() works when all params are null`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result("" + ctx.anyQueryParamNull("nullkey", "othernullkey")) }
        assertThat(http.getBody("/"), `is`("true"))
    }

    @Test
    fun `anyQueryParamNull() works when some params are null`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result("" + ctx.anyQueryParamNull("qp1", "qp2", "nullkey")) }
        assertThat(http.getBody("/?qp1=1&qp2=2"), `is`("true"))
    }

    @Test
    fun `anyQueryParamNull() works when all params are present`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result("" + ctx.anyQueryParamNull("qp1", "qp2", "qp3")) }
        assertThat(http.getBody("/?qp1=1&qp2=2&qp3=3"), `is`("false"))
    }

    /*
     * Form params
     */
    @Test
    fun `formParam() returns supplied form-param`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result("" + ctx.formParam("fp1")!!) }
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().body, `is`("1"))
    }

    @Test
    fun `formParam() returns null for unknown param`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result("" + ctx.formParam("fp3")) }
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().body, `is`("null"))
    }

    @Test
    fun `formParam() returns defaults to default value`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result("" + ctx.formParam("fp4", "4")!!) }
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().body, `is`("4"))
    }

    @Test
    fun `anyFormParamNull() works when some params are null`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result("" + ctx.anyFormParamNull("fp1", "fp2", "nullkey")) }
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().body, `is`("true"))
    }

    @Test
    fun `anyFormParamNull() works when all params are present`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result("" + ctx.anyFormParamNull("fp1", "fp2", "fp3")) }
        assertThat(http.post("/").body("fp1=1&fp2=2&fp3=3").asString().body, `is`("false"))
    }

    @Test
    fun `basicAuthCredentials() extracts username and password`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val basicAuthCredentials = ctx.basicAuthCredentials()
            ctx.result(basicAuthCredentials!!.username + "|" + basicAuthCredentials.password)
        }
        val response = Unirest.get("${http.origin}/").basicAuth("some-username", "some-password").asString()
        assertThat(response.body, `is`("some-username|some-password"))
    }

    @Test
    fun `matchedPath() returns the path used to match the request`() = TestUtil.test { app, http ->
        app.get("/matched") { ctx -> ctx.result(ctx.matchedPath()) }
        app.get("/matched/:path-param") { ctx -> ctx.result(ctx.matchedPath()) }
        app.after("/matched/:path-param/:param2") { ctx -> ctx.result(ctx.matchedPath()) }
        assertThat(http.getBody("/matched"), `is`("/matched"))
        assertThat(http.getBody("/matched/p1"), `is`("/matched/:path-param"))
        assertThat(http.getBody("/matched/p1/p2"), `is`("/matched/:path-param/:param2"))
    }

    @Test
    fun `servlet-context is not null`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(if (ctx.req.servletContext != null) "not-null" else "null") }
        assertThat(http.getBody("/"), `is`("not-null"))
    }

    /**
     * Param mapping
     */
    @Test
    fun `mapQueryParams() maps when all params are present`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val (name, email, phone) = ctx.mapQueryParams("name", "email", "phone") ?: throw IllegalArgumentException()
            ctx.result("$name|$email|$phone")
        }
        assertThat(http.getBody("/?name=some%20name&email=some%20email&phone=some%20phone"), `is`("some name|some email|some phone"))
    }

    @Test
    fun `mapQueryParams() throws when any param is missing`() = TestUtil.test { app, http ->
        app.get("/") { ctx ->
            val (name, missing) = ctx.mapQueryParams("name", "missing") ?: throw IllegalArgumentException()
            ctx.result("$name|$missing")
        }
        assertThat(http.getBody("/?name=some%20name"), `is`("Internal server error"))
    }

    @Test
    fun `mapFormParams() maps when all params are present`() = TestUtil.test { app, http ->
        app.post("/") { ctx ->
            val (name, email, phone) = ctx.mapFormParams("name", "email", "phone") ?: throw IllegalArgumentException()
            ctx.result("$name|$email|$phone")
        }
        assertThat(http.post("/").body("name=some%20name&email=some%20email&phone=some%20phone").asString().body, `is`("some name|some email|some phone"))
    }

    @Test
    fun `mapFormParams() throws when any param is missing`() = TestUtil.test { app, http ->
        app.post("/") { ctx ->
            val (name, missing) = ctx.mapFormParams("missing") ?: throw IllegalArgumentException()
            ctx.result("$name|$missing")
        }
        assertThat(http.post("/").body("name=some%20name").asString().body, `is`("Internal server error"))
    }

    /**
     * Simple proxy methods
     */
    @Test
    fun `contentLength() works`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result(ctx.contentLength().toString()) }
        assertThat(http.post("/").body("Hello").asString().body, `is`("5"))
    }

    @Test
    fun `host() works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.host()!!) }
        assertThat(http.getBody("/"), `is`("localhost:" + app.port()))
    }

    @Test
    fun `ip() works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.ip()) }
        assertThat(http.getBody("/"), `is`("127.0.0.1"))
    }

    @Test
    fun `protocol() works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.protocol()) }
        assertThat(http.getBody("/"), `is`("HTTP/1.1"))
    }

    @Test
    fun `scheme() works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.scheme()) }
        assertThat(http.getBody("/"), `is`("http"))
    }

    @Test
    fun `url() works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.url()) }
        assertThat(http.getBody("/"), `is`("http://localhost:" + app.port() + "/"))
    }

    @Test
    fun `userAgent() works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.userAgent()!!) }
        assertThat(http.getBody("/"), `is`("unirest-java/1.3.11"))
    }

}
