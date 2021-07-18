package io.javalin

import com.mashape.unirest.http.HttpMethod
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestServerHeader {

    @Test
    fun `server header is not set by default`() = TestUtil.test { app, http ->
        app.get("/hello") { ctx -> ctx.status(200).result("Hello world") }
        val response = http.call(HttpMethod.GET, "/hello")
        assertThat(response.headers.getFirst("Server")).isNull()
    }
}