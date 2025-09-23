package io.javalin

import io.javalin.http.HttpStatus.METHOD_NOT_ALLOWED
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import kong.unirest.HttpMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestAllowHeaderComprehensive {

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
    fun `405 response works with JSON content type`() {
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
}