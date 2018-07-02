package io.javalin

import io.javalin.core.util.Header
import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test
import javax.servlet.http.HttpServletResponse

class TestMethodNotAllowed {

    private val preferring405Javalin = Javalin.create()
            .prefer405over404()
            .get("/test") { ctx -> ctx.result("Hello world") }
            .put("/test") { ctx -> ctx.result("Hello world") }
            .delete("/test") { ctx -> ctx.result("Hello world") }

    private val expectedHtml = """
            |<!DOCTYPE html>
            |<html lang="en">
            |    <head>
            |        <meta charset="UTF-8">
            |        <title>Method Not Allowed</title>
            |    </head>
            |    <body>
            |        <h1>405 - Method Not Allowed</h1>
            |        <p>
            |            Available Methods: <strong>GET, PUT, DELETE</strong>
            |        </p>
            |    </body>
            |</html>""".trimMargin()

    private val expectedJson = """{"availableMethods":["GET", "PUT", "DELETE"]}"""

    @Test
    fun test_htmlMethodNotAllowed() = TestUtil(preferring405Javalin).test { app, http ->
        val response = http.post("/test").header(Header.ACCEPT, "text/html").asString()
        assertThat(response.status, `is`(HttpServletResponse.SC_METHOD_NOT_ALLOWED))
        assertThat(response.body, `is`(expectedHtml))
    }

    @Test
    fun test_jsonMethodNotAllowed() = TestUtil(preferring405Javalin).test { app, http ->
        val response = http.post("/test").header(Header.ACCEPT, "application/json").asString()
        assertThat(response.status, `is`(HttpServletResponse.SC_METHOD_NOT_ALLOWED))
        assertThat(response.body, `is`(expectedJson))
    }

}
