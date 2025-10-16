package io.javalin

import io.javalin.http.Header
import io.javalin.http.HttpStatus.METHOD_NOT_ALLOWED
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import kong.unirest.HttpMethod
import kong.unirest.HttpResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestMethodNotAllowed {

    private val preferring405Javalin = Javalin.create { config ->
        config.http.prefer405over404 = true
        config.routes.post("/test") { it.result("Hello world") }
        config.routes.put("/test") { it.result("Hello world") }
        config.routes.delete("/test") { it.result("Hello world") }
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
    fun `Allow header contains all available HTTP methods`() {
        val app = Javalin.create { config ->
            config.http.prefer405over404 = true
            config.routes.get("/api") { ctx -> ctx.result("GET response") }
            config.routes.post("/api") { ctx -> ctx.result("POST response") }
            config.routes.patch("/api") { ctx -> ctx.result("PATCH response") }
        }
        TestUtil.test(app) { _, http ->
            val response = http.call(HttpMethod.PUT, "/api")
            assertThat(response.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
            assertThat(response.allowHeader.split(", ")).containsExactlyInAnyOrder("GET", "POST", "PATCH")
        }
    }

    @Test
    fun `Allow header works for single available method`() {
        val app = Javalin.create { config ->
            config.http.prefer405over404 = true
            config.routes.delete("/single") { ctx -> ctx.result("DELETE response") }
        }
        TestUtil.test(app) { _, http ->
            val response = http.get("/single")
            assertThat(response.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
            assertThat(response.allowHeader).isEqualTo("DELETE")
        }
    }

    @Test
    fun `Allow header is present in JSON 405 responses`() {
        val app = Javalin.create { config ->
            config.http.prefer405over404 = true
            config.routes.post("/json") { ctx -> ctx.result("POST response") }
            config.routes.put("/json") { ctx -> ctx.result("PUT response") }
        }
        TestUtil.test(app) { _, http ->
            val response = http.jsonGet("/json")
            assertThat(response.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
            assertThat(response.allowHeader.split(", ")).containsExactlyInAnyOrder("POST", "PUT")
        }
    }

    @Test
    fun `Allow header is present in HTML 405 responses`() {
        val app = Javalin.create { config ->
            config.http.prefer405over404 = true
            config.routes.get("/html") { ctx -> ctx.result("GET response") }
            config.routes.post("/html") { ctx -> ctx.result("POST response") }
        }
        TestUtil.test(app) { _, http ->
            val response = http.call(HttpMethod.PUT, "/html")
            assertThat(response.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
            assertThat(response.allowHeader).contains("GET", "POST")
        }
    }

    @Test
    fun `Allow header is present in plain text 405 responses`() {
        val app = Javalin.create { config ->
            config.http.prefer405over404 = true
            config.routes.get("/plain") { ctx -> ctx.result("GET response") }
            config.routes.put("/plain") { ctx -> ctx.result("PUT response") }
        }
        TestUtil.test(app) { _, http ->
            val response = http.call(HttpMethod.DELETE, "/plain")
            assertThat(response.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
            assertThat(response.allowHeader).contains("GET", "PUT")
        }
    }

    @Test
    fun `Allow header is not set for 404 responses`() = TestUtil.test { app, http ->
        app.unsafe.routes.get("/test") { it.result("Hello") }
        val response = http.call(HttpMethod.POST, "/test")
        assertThat(response.status).isEqualTo(404)
        assertThat(response.headers["Allow"]).isNullOrEmpty()
    }

    private val HttpResponse<*>.allowHeader: String get() = this.headers[Header.ALLOW]!![0]

}
