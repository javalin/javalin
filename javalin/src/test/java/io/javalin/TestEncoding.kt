/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.core.util.Header
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URLEncoder

class TestEncoding {

    @Test
    fun `unicode path-params work`() = TestUtil.test { app, http ->
        app.get("/:path-param") { ctx -> ctx.result(ctx.pathParam("path-param")) }
        assertThat(http.getBody("/æøå")).isEqualTo("æøå")
        assertThat(http.getBody("/♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")).isEqualTo("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")
        assertThat(http.getBody("/こんにちは")).isEqualTo("こんにちは")
    }

    @Test
    fun `unicode query-params work`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.queryParam("qp")!!) }
        assertThat(http.getBody("/?qp=æøå")).isEqualTo("æøå")
        assertThat(http.getBody("/?qp=♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")).isEqualTo("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")
        assertThat(http.getBody("/?qp=こんにちは")).isEqualTo("こんにちは")
    }

    @Test
    fun `URLEncoded query-params work utf-8`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.queryParam("qp")!!) }
        assertThat(http.getBody("/?qp=" + "8%3A00+PM")).isEqualTo("8:00 PM")
        val encoded = URLEncoder.encode("!#$&'()*+,/:;=?@[]", "UTF-8")
        assertThat(http.getBody("/?qp=$encoded")).isEqualTo("!#$&'()*+,/:;=?@[]")
    }

    @Test
    fun `URLEncoded query-params work ISO-8859-1`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.result(ctx.queryParam("qp")!!) }
        val encoded = URLEncoder.encode("æøå", "ISO-8859-1")
        val response = Unirest.get(http.origin + "/?qp=$encoded").header("Content-Type", "text/plain; charset=ISO-8859-1").asString()
        assertThat(response.body).isEqualTo("æøå")
    }

    @Test
    fun `URLEncoded form-params work`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result(ctx.formParam("qp")!!) }
        val response = Unirest
                .post(http.origin)
                .body("qp=8%3A00+PM")
                .asString()
        assertThat(response.body).isEqualTo("8:00 PM")
    }

    @Test
    fun `default charsets work`() = TestUtil.test { app, http ->
        app.get("/text") { ctx -> ctx.result("суп из капусты") }
        app.get("/json") { ctx -> ctx.json("白菜湯") }
        app.get("/html") { ctx -> ctx.html("kålsuppe") }
        assertThat(http.get("/text").headers.getFirst(Header.CONTENT_TYPE)).isEqualTo("text/plain")
        assertThat(http.get("/json").headers.getFirst(Header.CONTENT_TYPE)).isEqualTo("application/json")
        assertThat(http.get("/html").headers.getFirst(Header.CONTENT_TYPE)).isEqualTo("text/html")
        assertThat(http.getBody("/text")).isEqualTo("суп из капусты")
        assertThat(http.getBody("/json")).isEqualTo("\"白菜湯\"")
        assertThat(http.getBody("/html")).isEqualTo("kålsuppe")
    }

    @Test
    fun `setting a default content-type works`() = TestUtil.test(Javalin.create { it.defaultContentType = "application/json" }) { app, http ->
        app.get("/default") { ctx -> ctx.result("not json") }
        assertThat(http.get("/default").headers.getFirst(Header.CONTENT_TYPE)).contains("application/json")
    }

    @Test
    fun `content-type can be overridden in handler`() = TestUtil.test(Javalin.create { it.defaultContentType = "application/json" }) { app, http ->
        app.get("/override") { ctx ->
            ctx.res.characterEncoding = "utf-8"
            ctx.res.contentType = "text/html"
        }
        assertThat(http.get("/override").headers.getFirst(Header.CONTENT_TYPE)).contains("utf-8")
        assertThat(http.get("/override").headers.getFirst(Header.CONTENT_TYPE)).contains("text/html")
    }

}
