package io.javalin

import io.javalin.http.HttpStatus.METHOD_NOT_ALLOWED
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestAllowHeader {

    private val preferring405Javalin = Javalin.create { it.http.prefer405over404 = true }.apply {
        post("/test") { it.result("Hello world") }
        put("/test") { it.result("Hello world") }
        delete("/test") { it.result("Hello world") }
    }

    @Test
    fun `405 response includes Allow header`() = TestUtil.test(preferring405Javalin) { _, http ->
        val response = http.get("/test")
        assertThat(response.httpCode()).isEqualTo(METHOD_NOT_ALLOWED)
        
        // Print all headers to see what's currently there
        println("Headers: ${response.headers}")
        println("Allow header: ${response.headers["Allow"]}")
        
        // Check if Allow header exists
        assertThat(response.headers["Allow"]).isNotNull()
        assertThat(response.headers["Allow"]?.get(0)).contains("POST", "PUT", "DELETE")
    }
}