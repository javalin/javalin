/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin

import io.javalin.http.ContentType
import io.javalin.http.Header
import io.javalin.http.HttpStatus
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URLEncoder

class TestEncoding {

    @Test
    fun `unicode path-params work`() = TestUtil.test { app, http ->
        app.get("/{path-param}") { it.result(it.pathParam("path-param")) }
        assertThat(http.getStatus("/æøå")).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/æøå")).isEqualTo("æøå")
        assertThat(http.getBody("/♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")).isEqualTo("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")
        assertThat(http.getBody("/こんにちは")).isEqualTo("こんにちは")
    }

    @Test
    fun `unicode query-params work`() = TestUtil.test { app, http ->
        app.get("/") { it.result(it.queryParam("qp")!!) }
        assertThat(http.getStatus("/?qp=æøå")).isEqualTo(HttpStatus.OK)
        assertThat(http.getBody("/?qp=æøå")).isEqualTo("æøå")
        assertThat(http.getBody("/?qp=♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")).isEqualTo("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟")
        assertThat(http.getBody("/?qp=こんにちは")).isEqualTo("こんにちは")
    }

    @Test
    fun `URLEncoded query-params work utf-8`() = TestUtil.test { app, http ->
        app.get("/") { it.result(it.queryParam("qp")!!) }
        assertThat(http.getBody("/?qp=" + "8%3A00+PM")).isEqualTo("8:00 PM")
        val encoded = URLEncoder.encode("!#$&'()*+,/:;=?@[]", "UTF-8")
        assertThat(http.getBody("/?qp=$encoded")).isEqualTo("!#$&'()*+,/:;=?@[]")
    }

    @Test
    fun `URLEncoded query-params work ISO-8859-1`() = TestUtil.test { app, http ->
        app.get("/") { it.result(it.queryParam("qp")!!) }
        val encoded = URLEncoder.encode("æøå", "ISO-8859-1")
        val response = http.call("GET", "/?qp=$encoded", mapOf("Content-Type" to "text/plain; charset=ISO-8859-1"))
        assertThat(response.body).isEqualTo("æøå")
    }

    @Test
    fun `URLEncoded form-params work`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.formParam("qp")!!) }
        val response = Unirest
            .post(http.origin)
            .body("qp=8%3A00+PM")
            .asString()
        assertThat(response.body).isEqualTo("8:00 PM")
    }

    @Test
    fun `default charsets work`() = TestUtil.test { app, http ->
        app.get("/text") { it.result("суп из капусты") }
        app.get("/json") { it.json("白菜湯") }
        app.get("/html") { it.html("kålsuppe") }
        assertThat(http.get("/text").headers.getFirst(Header.CONTENT_TYPE)).isEqualTo(ContentType.PLAIN)
        assertThat(http.get("/json").headers.getFirst(Header.CONTENT_TYPE)).isEqualTo(ContentType.JSON)
        assertThat(http.get("/html").headers.getFirst(Header.CONTENT_TYPE)).isEqualTo(ContentType.HTML)
        assertThat(http.getBody("/text")).isEqualTo("суп из капусты")
        assertThat(http.getBody("/json")).isEqualTo("白菜湯")
        assertThat(http.getBody("/html")).isEqualTo("kålsuppe")
    }

    @Test
    fun `setting a default content-type works`() = TestUtil.test(Javalin.create { it.http.defaultContentType = ContentType.JSON }) { app, http ->
        app.get("/default") { it.result("not json") }
        assertThat(http.get("/default").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.JSON)
    }

    @Test
    fun `content-type can be overridden in handler`() = TestUtil.test(Javalin.create { it.http.defaultContentType = ContentType.JSON }) { app, http ->
        app.get("/override") { ctx ->
            ctx.res().characterEncoding = "utf-8"
            ctx.res().contentType = ContentType.HTML
        }
        assertThat(http.get("/override").headers.getFirst(Header.CONTENT_TYPE)).contains("utf-8")
        assertThat(http.get("/override").headers.getFirst(Header.CONTENT_TYPE)).contains(ContentType.HTML)
    }

    @Test
    fun `URLEncoded form-params work Windows-1252`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.formParam("fp")!!) }
        val response = http.post("/")
            .header(Header.CONTENT_TYPE, "text/plain; charset=Windows-1252")
            .body("fp=${URLEncoder.encode("æøå", "Windows-1252")}")
            .asString()
        assertThat(response.body).isEqualTo("æøå")
    }

    @Test
    fun `URLEncoded form-params work Windows-1252 alt`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.formParam("fp")!!) }
        val response = http.post("/")
            .header(Header.CONTENT_ENCODING, "text/plain; charset=Windows-1252")
            .field("fp", "æøå")
            .asString()
        assertThat(response.body).isEqualTo("æøå")
    }

}
