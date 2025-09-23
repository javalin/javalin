package io.javalin

import io.javalin.http.HttpStatus.METHOD_NOT_ALLOWED
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
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
        assertThat(http.htmlGet("/test").httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
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
        assertThat(http.jsonGet("/test").httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
        assertThat(http.jsonGet("/test").body).isEqualTo(expectedJson)
    }

    @Test
    fun `405 response includes Allow header`() = TestUtil.test(preferring405Javalin) { _, http ->
        val response = http.get("/test")
        assertThat(response.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
        assertThat(response.headers["Allow"]).isNotNull()
        assertThat(response.headers["Allow"]!!.first()).isEqualTo("POST, PUT, DELETE")
    }

}
