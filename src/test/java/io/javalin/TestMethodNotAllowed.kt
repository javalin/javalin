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
            |Method not allowed
            |
            |Available methods:
            |GET, PUT, DELETE
            |""".trimMargin()

    private val expectedJson = """{
    |    "title": "Method not allowed",
    |    "status": 405,
    |    "type": "https//javalin.io/documentation#MethodNotAllowedResponse",
    |    "details": {availableMethods=["GET", "PUT", "DELETE"]}}
    |}""".trimMargin()

    @Test
    fun `405 response for HTML works`() = TestUtil.test(preferring405Javalin) { app, http ->
        val response = http.post("/test").header(Header.ACCEPT, "text/html").asString()
        assertThat(response.status, `is`(HttpServletResponse.SC_METHOD_NOT_ALLOWED))
        assertThat(response.body, `is`(expectedHtml))
    }

    @Test
    fun `405 response for JSON works`() = TestUtil.test(preferring405Javalin) { app, http ->
        val response = http.post("/test").header(Header.ACCEPT, "application/json").asString()
        assertThat(response.status, `is`(HttpServletResponse.SC_METHOD_NOT_ALLOWED))
        assertThat(response.body, `is`(expectedJson))
    }

}
