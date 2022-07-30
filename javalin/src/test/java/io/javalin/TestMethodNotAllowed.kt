package io.javalin

import io.javalin.http.HttpCode.METHOD_NOT_ALLOWED
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestMethodNotAllowed {

    private val preferring405Javalin = Javalin.create { it.http.prefer405over404 = true }.apply {
        post("/test") { it.result("Hello world") }
        put("/test") { it.result("Hello world") }
        delete("/test") { it.result("Hello world") }
    }

    @Test
    fun `405 response for HTML works`() = TestUtil.test(preferring405Javalin) { _, http ->
        val expectedHtml = """
            |Method Not Allowed
            |
            |Available methods:
            |POST, PUT, DELETE
            |""".trimMargin()
        assertThat(http.htmlGet("/test").status).isEqualTo(METHOD_NOT_ALLOWED.status)
        assertThat(http.htmlGet("/test").body).isEqualTo(expectedHtml)
    }

    @Test
    fun `405 response for JSON works`() = TestUtil.test(preferring405Javalin) { _, http ->
        val expectedJson = """{
            |    "title": "Method Not Allowed",
            |    "status": 405,
            |    "type": "https://javalin.io/documentation#methodnotallowedresponse",
            |    "details": {"availableMethods":"POST, PUT, DELETE"}
            |}""".trimMargin()
        assertThat(http.jsonGet("/test").status).isEqualTo(METHOD_NOT_ALLOWED.status)
        assertThat(http.jsonGet("/test").body).isEqualTo(expectedJson)
    }

}
