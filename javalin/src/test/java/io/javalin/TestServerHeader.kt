package io.javalin

import io.javalin.testing.TestUtil
import kong.unirest.HttpMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestServerHeader {

    @Test
    fun `server header is not set by default`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.status(200).result("Hello world") }
        val response = http.call(HttpMethod.GET, "/hello")
        assertThat(response.headers.getFirst("Server")).isEqualTo("")
    }
}
