package io.javalin

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import javax.servlet.http.HttpServletResponse

class TestMethodNotAllowed {

    private val preferring405Javalin = Javalin.create { it.prefer405over404 = true }.apply {
        post("/test") { ctx -> ctx.result("Hello world") }
        put("/test") { ctx -> ctx.result("Hello world") }
        delete("/test") { ctx -> ctx.result("Hello world") }
    }

    private val expectedHtml = """
            |Method not allowed
            |
            |Available methods:
            |POST, PUT, DELETE
            |""".trimMargin()

    private val expectedJson = """{
    |    "title": "Method not allowed",
    |    "status": 405,
    |    "type": "https://javalin.io/documentation#methodnotallowedresponse",
    |    "details": [{"availableMethods": "POST, PUT, DELETE"}]
    |}""".trimMargin()

    @Test
    fun `405 response for HTML works`() = TestUtil.test(preferring405Javalin) { _, http ->
        assertThat(http.htmlGet("/test").status).isEqualTo(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
        assertThat(http.htmlGet("/test").body).isEqualTo(expectedHtml)
    }

    @Test
    fun `405 response for JSON works`() = TestUtil.test(preferring405Javalin) { _, http ->
        assertThat(http.jsonGet("/test").status).isEqualTo(HttpServletResponse.SC_METHOD_NOT_ALLOWED)
        assertThat(http.jsonGet("/test").body).isEqualTo(expectedJson)
    }

}
