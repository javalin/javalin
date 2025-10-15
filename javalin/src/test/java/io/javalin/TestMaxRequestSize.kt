package io.javalin

import io.javalin.config.HttpConfig
import io.javalin.config.JavalinConfig
import io.javalin.http.HttpStatus.CONTENT_TOO_LARGE
import io.javalin.http.HttpStatus.OK
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import io.javalin.testing.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestMaxRequestSize {

    @Test
    fun `max request size is set by default`() = TestUtil.test { app, http ->
        val size = HttpConfig(JavalinConfig()).maxRequestSize.toInt()
        app.post("/") { it.result(it.body()) }
        assertThat(http.post("/").body(ByteArray(size)).asString().httpCode()).isEqualTo(OK)
        try {
            val response = http.post("/").body(ByteArray(size + 1)).asString()
            assertThat(response.httpCode()).isEqualTo(CONTENT_TOO_LARGE)
        } catch (e: Exception) {
            // if the content is too large, the client may get disconnected before receiving a response
            assertThat(e.cause).isInstanceOf(java.net.SocketException::class.java)
        }
    }

    @Test
    fun `user can configure max request size`() = TestUtil.test(Javalin.create { it.http.maxRequestSize = 4L }) { app, http ->
        app.post("/") { it.result(it.body()) }
        assertThat(http.post("/").body(ByteArray(4)).asString().httpCode()).isEqualTo(OK)
        assertThat(http.post("/").body(ByteArray(5)).asString().httpCode()).isEqualTo(CONTENT_TOO_LARGE)
    }

    @Test
    fun `body can be read multiple times`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.body() + it.body() + it.body()) }
        assertThat(http.post("/").body("Hi").asString().body).isEqualTo("HiHiHi")
    }

    @Test
    fun `can read payloads larger than max size by using inputstream`() = TestUtil.test(Javalin.create {
        it.http.maxRequestSize = 4L
        it.routes.post("/body") { it.result(it.body()) }
        it.routes.post("/stream") { it.result(it.req().inputStream.readBytes()) }
    }) { _, http ->
        assertThat(http.post("/body").body("123456").asString().body).isEqualTo("Content Too Large")
        assertThat(http.post("/stream").body("123456").asString().body).isEqualTo("123456")
    }

}
