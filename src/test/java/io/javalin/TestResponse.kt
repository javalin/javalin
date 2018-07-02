/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.HttpMethod
import io.javalin.util.BaseTest
import org.apache.commons.io.IOUtils
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.hamcrest.core.IsCollectionContaining.hasItems
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*

class TestResponse : BaseTest() {

    private val MY_BODY = (""
            + "This is my body, and I live in it. It's 31 and 6 months old. "
            + "It's changed a lot since it was new. It's done stuff it wasn't built to do. "
            + "I often try to fill if up with wine. - Tim Minchin")

    @Test
    fun test_resultString() {
        app.get("/hello") { ctx ->
            ctx.status(418).result(MY_BODY).header("X-HEADER-1", "my-header-1").header("X-HEADER-2", "my-header-2")
        }
        val response = http.call(HttpMethod.GET, "/hello")
        assertThat(response.status, `is`(418))
        assertThat(response.body, `is`(MY_BODY))
        assertThat(response.headers.getFirst("X-HEADER-1"), `is`("my-header-1"))
        assertThat(response.headers.getFirst("X-HEADER-2"), `is`("my-header-2"))
    }

    @Test
    fun test_resultStream() {
        val buf = ByteArray(65537) // big and not on a page boundary
        Random().nextBytes(buf)
        app.get("/stream") { ctx -> ctx.result(ByteArrayInputStream(buf)) }
        val response = http.call(HttpMethod.GET, "/stream")
        val bout = ByteArrayOutputStream()
        assertThat(IOUtils.copy(response.rawBody, bout), `is`(buf.size))
        assertThat(buf, equalTo(bout.toByteArray()))
    }

    @Test
    fun test_redirectInBefore() {
        app.before("/before") { ctx -> ctx.redirect("/redirected") }
        app.get("/redirected") { ctx -> ctx.result("Redirected") }
        assertThat(http.getBody("/before"), `is`("Redirected"))
    }

    @Test
    fun test_redirectInExceptionMapper() {
        app.get("/get") { ctx -> throw Exception() }
        app.exception(Exception::class.java) { exception, ctx -> ctx.redirect("/redirected") }
        app.get("/redirected") { ctx -> ctx.result("Redirected") }
        assertThat(http.getBody("/get"), `is`("Redirected"))
    }

    @Test
    fun test_redirect() {
        app.get("/hello") { ctx -> ctx.redirect("/hello-2") }
        app.get("/hello-2") { ctx -> ctx.result("Redirected") }
        assertThat(http.getBody("/hello"), `is`("Redirected"))
    }

    @Test
    fun test_redirectWithStatus() {
        app.get("/hello") { ctx -> ctx.redirect("/hello-2", 301) }
        app.get("/hello-2") { ctx -> ctx.result("Redirected") }
        http.disableUnirestRedirects()
        assertThat(http.call(HttpMethod.GET, "/hello").status, `is`(301))
        http.enableUnirestRedirects()
        assertThat(http.call(HttpMethod.GET, "/hello").body, `is`("Redirected"))
    }

    @Test
    fun test_redirectWithStatus_absolutePath() {
        app.get("/hello-abs") { ctx -> ctx.redirect("$origin/hello-abs-2", 303) }
        app.get("/hello-abs-2") { ctx -> ctx.result("Redirected") }
        http.disableUnirestRedirects()
        assertThat(http.call(HttpMethod.GET, "/hello-abs").status, `is`(303))
        http.enableUnirestRedirects()
        assertThat(http.call(HttpMethod.GET, "/hello-abs").body, `is`("Redirected"))
    }

    @Test
    fun test_createCookie() {
        app.post("/create-cookies") { ctx -> ctx.cookie("name1", "value1").cookie("name2", "value2") }
        assertThat<List<String>>(http.post("/create-cookies").asString().headers["Set-Cookie"], hasItems("name1=value1", "name2=value2"))
    }

    @Test
    fun test_cookie() {
        app.post("/create-cookie") { ctx -> ctx.cookie("Test", "Tast") }
        assertThat<List<String>>(http.post("/create-cookie").asString().headers["Set-Cookie"], hasItem("Test=Tast"))
    }

}
