package io.javalin

import io.javalin.util.TestUtil
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.Test
import javax.servlet.http.HttpServletResponse

class TestMethodNotAllowed {

    private val preferring405Javalin = Javalin.create()
            .prefer405over404()
            .post("/test") { ctx -> ctx.result("Hello world") }
            .put("/test") { ctx -> ctx.result("Hello world") }
            .delete("/test") { ctx -> ctx.result("Hello world") }

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
    fun `405 response for HTML works`() = TestUtil.test(preferring405Javalin) { app, http ->
        assertThat(http.htmlGet("/test").status, `is`(HttpServletResponse.SC_METHOD_NOT_ALLOWED))
        assertThat(http.htmlGet("/test").body, `is`(expectedHtml))
    }

    @Test
    fun `405 response for JSON works`() = TestUtil.test(preferring405Javalin) { app, http ->
        assertThat(http.jsonGet("/test").status, `is`(HttpServletResponse.SC_METHOD_NOT_ALLOWED))
        assertThat(http.jsonGet("/test").body, `is`(expectedJson))
    }

}
