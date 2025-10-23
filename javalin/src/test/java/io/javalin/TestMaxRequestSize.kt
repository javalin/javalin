package io.javalin

import io.javalin.config.HttpConfig
import io.javalin.config.JavalinConfig
import io.javalin.http.HttpStatus.CONTENT_TOO_LARGE
import io.javalin.http.HttpStatus.OK
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.IOException

class TestMaxRequestSize {

    @Test
    fun `max request size is set by default`() = TestUtil.test { app, http ->
        val size = HttpConfig(JavalinConfig()).maxRequestSize.toInt()
        app.unsafe.routes.post("/") { it.result(it.body()) }
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
        app.unsafe.routes.post("/") { it.result(it.body()) }
        assertThat(http.post("/").body(ByteArray(4)).asString().httpCode()).isEqualTo(OK)
        assertThat(http.post("/").body(ByteArray(5)).asString().httpCode()).isEqualTo(CONTENT_TOO_LARGE)
    }

    @Test
    fun `body can be read multiple times`() = TestUtil.test { app, http ->
        app.unsafe.routes.post("/") { it.result(it.body() + it.body() + it.body()) }
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

    @Test
    fun `enforces max size when reading body with unknown content length`() {
        val app = Javalin.create { it.http.maxRequestSize = 100L }
        app.unsafe.routes.post("/test") { it.result(it.body()) }

        TestUtil.test(app) { _, http ->
            val client = OkHttpClient()

            // Test 1: Small chunked body (50 bytes) should work
            val smallBody = "x".repeat(50)
            val smallRequest = Request.Builder()
                .url("${http.origin}/test")
                .post(createBufferedRequest(smallBody))
                .build()

            client.newCall(smallRequest).execute().use { response ->
                assertThat(response.code).isEqualTo(200)
                assertThat(response.body?.string()).isEqualTo(smallBody)
            }

            // Test 2: Large chunked body (200 bytes) should be rejected
            val largeBody = "x".repeat(200)
            val largeRequest = Request.Builder()
                .url("${http.origin}/test")
                .post(createBufferedRequest(largeBody))
                .build()

            val largeResponse = client.newCall(largeRequest).execute()
            assertThat(largeResponse.code).isEqualTo(413) // CONTENT_TOO_LARGE
            largeResponse.close()
        }
    }

    fun createBufferedRequest(body: String) = object : RequestBody() {
        override fun contentType() = "text/plain".toMediaType()
        override fun contentLength() = body.length.toLong()
        override fun writeTo(sink: BufferedSink) {
            sink.writeUtf8(body)
        }
    }

}
