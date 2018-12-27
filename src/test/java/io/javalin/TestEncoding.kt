/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.core.util.Header
import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsString
import org.junit.Test
import java.net.URLEncoder

class TestEncoding {

    @Test
    fun `unicode path-params work`() = TestUtil.test { app, http ->
        app.get("/:path-param") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        assertThat(http.getBody("/æøå"), `is`("æøå"))
        assertThat(http.getBody("/♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"), `is`("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"))
        assertThat(http.getBody("/こんにちは"), `is`("こんにちは"))
    }

    @Test
    fun `unicode query-params work`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.queryParam("qp")!!) }
        assertThat(http.getBody("/?qp=æøå"), `is`("æøå"))
        assertThat(http.getBody("/?qp=♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"), `is`("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"))
        assertThat(http.getBody("/?qp=こんにちは"), `is`("こんにちは"))
    }

    @Test
    fun `URLEncoded query-params work`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.queryParam("qp")!!) }
        assertThat(http.getBody("/?qp=" + "8%3A00+PM"), `is`("8:00 PM"))
        val encoded = URLEncoder.encode("!#$&'()*+,/:;=?@[]", "UTF-8")
        assertThat(http.getBody("/?qp=$encoded"), `is`("!#$&'()*+,/:;=?@[]"))
    }

    @Test
    fun `URLEncoded form-params work`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result(ctx.formParam("qp")!!) }
        val response = Unirest
                .post(http.origin)
                .body("qp=8%3A00+PM")
                .asString()
        assertThat(response.body, `is`("8:00 PM"))
    }

    @Test
    fun `default charsets work`() = TestUtil.test { app, http ->
        app.get("/text") { ctx -> ctx.result("суп из капусты") }
        app.get("/json") { ctx -> ctx.json("白菜湯") }
        app.get("/html") { ctx -> ctx.html("kålsuppe") }
        assertThat(http.get("/text").headers.getFirst(Header.CONTENT_TYPE), `is`("text/plain"))
        assertThat(http.get("/json").headers.getFirst(Header.CONTENT_TYPE), `is`("application/json"))
        assertThat(http.get("/html").headers.getFirst(Header.CONTENT_TYPE), `is`("text/html"))
        assertThat(http.getBody("/text"), `is`("суп из капусты"))
        assertThat(http.getBody("/json"), `is`("\"白菜湯\""))
        assertThat(http.getBody("/html"), `is`("kålsuppe"))
    }

    @Test
    fun `setting a default content-type works`() = TestUtil.test(Javalin.create().defaultContentType("application/json")) { app, http ->
        app.get("/default") { ctx -> ctx.result("not json") }
        assertThat(http.get("/default").headers.getFirst(Header.CONTENT_TYPE), containsString("application/json"))
    }

    @Test
    fun `content-type can be overridden in handler`() = TestUtil.test(Javalin.create().defaultContentType("application/json")) { app, http ->
        app.get("/override") { ctx ->
            ctx.res.characterEncoding = "utf-8"
            ctx.res.contentType = "text/html"
        }
        assertThat(http.get("/override").headers.getFirst(Header.CONTENT_TYPE), containsString("utf-8"))
        assertThat(http.get("/override").headers.getFirst(Header.CONTENT_TYPE), containsString("text/html"))
    }

}
