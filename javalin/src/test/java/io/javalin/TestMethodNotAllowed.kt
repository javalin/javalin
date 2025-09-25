package io.javalin

import io.javalin.http.HttpStatus.METHOD_NOT_ALLOWED
import io.javalin.http.MethodNotAllowedResponse
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import kong.unirest.HttpMethod
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

    @Test
    fun `405 response includes Allow header for different HTTP methods`() {
        val app = Javalin.create { it.http.prefer405over404 = true }.apply {
            get("/api") { ctx -> ctx.result("GET response") }
            post("/api") { ctx -> ctx.result("POST response") }
            patch("/api") { ctx -> ctx.result("PATCH response") }
        }
        
        TestUtil.test(app) { _, http ->
            val response = http.call(HttpMethod.PUT, "/api")
            assertThat(response.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
            assertThat(response.headers["Allow"]).isNotNull()
            val allowHeader = response.headers["Allow"]!![0]
            assertThat(allowHeader).contains("GET")
            assertThat(allowHeader).contains("POST")
            assertThat(allowHeader).contains("PATCH")
        }
    }

    @Test
    fun `405 response includes Allow header for single HTTP method`() {
        val app = Javalin.create { it.http.prefer405over404 = true }.apply {
            delete("/single") { ctx -> ctx.result("DELETE response") }
        }
        
        TestUtil.test(app) { _, http ->
            val response = http.get("/single")
            assertThat(response.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
            assertThat(response.headers["Allow"]).isNotNull()
            assertThat(response.headers["Allow"]!![0]).isEqualTo("DELETE")
        }
    }

    @Test
    fun `405 response includes Allow header with JSON content type`() {
        val app = Javalin.create { it.http.prefer405over404 = true }.apply {
            post("/json") { ctx -> ctx.result("POST response") }
            put("/json") { ctx -> ctx.result("PUT response") }
        }
        
        TestUtil.test(app) { _, http ->
            val response = http.jsonGet("/json")
            assertThat(response.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
            assertThat(response.headers["Allow"]).isNotNull()
            val allowHeader = response.headers["Allow"]!![0]
            assertThat(allowHeader).contains("POST")
            assertThat(allowHeader).contains("PUT")
        }
    }

    @Test
    fun `405 response includes Allow header with HTML content type`() {
        val app = Javalin.create { it.http.prefer405over404 = true }.apply {
            get("/html") { ctx -> ctx.result("GET response") }
            post("/html") { ctx -> ctx.result("POST response") }
        }
        
        TestUtil.test(app) { _, http ->
            // Use PUT method to get 405 since GET and POST are defined
            val response = http.call(HttpMethod.PUT, "/html")
            assertThat(response.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
            assertThat(response.headers["Allow"]).isNotNull()
            val allowHeader = response.headers["Allow"]!![0]
            assertThat(allowHeader).contains("GET")
            assertThat(allowHeader).contains("POST")
        }
    }

    @Test
    fun `405 response includes Allow header with plain content type`() {
        val app = Javalin.create { it.http.prefer405over404 = true }.apply {
            get("/plain") { ctx -> ctx.result("GET response") }
            put("/plain") { ctx -> ctx.result("PUT response") }
        }
        
        TestUtil.test(app) { _, http ->
            val response = http.call(HttpMethod.DELETE, "/plain")
            assertThat(response.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
            assertThat(response.headers["Allow"]).isNotNull()
            val allowHeader = response.headers["Allow"]!![0]
            assertThat(allowHeader).contains("GET")
            assertThat(allowHeader).contains("PUT")
        }
    }

    @Test 
    fun `Allow header not set when no method details available`() = TestUtil.test { app, http ->
        // Create app without prefer405over404 so it returns 404 by default  
        app.get("/test") { it.result("Hello") }
        
        val response = http.call(HttpMethod.POST, "/test")
        // Without prefer405over404, this will be 404, not 405
        assertThat(response.status).isEqualTo(404)
        // Allow header should not be set for 404
        assertThat(response.headers["Allow"]).isNullOrEmpty()
    }

}
