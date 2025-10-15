package io.javalin

import io.javalin.http.HttpStatus.OK
import io.javalin.testing.TestUtil
import io.javalin.testing.get
import kong.unirest.HttpMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestServerHeader {

    @Test
    fun `server header is not set by default`() = TestUtil.test { app, http ->
        app.get("/hello") { it.status(OK).result("Hello world") }
        val response = http.call(HttpMethod.GET, "/hello")
        assertThat(response.headers.getFirst("Server")).isEqualTo("")
    }
}
