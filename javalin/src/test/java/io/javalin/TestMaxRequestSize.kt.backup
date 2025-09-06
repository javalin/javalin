package io.javalin

import io.javalin.http.HttpStatus.CONTENT_TOO_LARGE
import io.javalin.http.HttpStatus.OK
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestMaxRequestSize {

    @Test
    fun `max request size is set by default`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.body()) }
        assertThat(http.post("/").body(ByteArray(1_000_000)).asString().httpCode()).isEqualTo(OK)
        assertThat(http.post("/").body(ByteArray(1_000_001)).asString().httpCode()).isEqualTo(CONTENT_TOO_LARGE)
    }

    @Test
    fun `user can configure max request size`() = TestUtil.test(Javalin.create { it.http.maxRequestSize = 4L }) { app, http ->
        app.post("/") { it.result(it.body()) }
        assertThat(http.post("/").body(ByteArray(4)).asString().httpCode()).isEqualTo(OK)
        assertThat(http.post("").body(ByteArray(5)).asString().httpCode()).isEqualTo(CONTENT_TOO_LARGE)
    }

    @Test
    fun `body can be read multiple times`() = TestUtil.test(Javalin.create()) { app, http ->
        app.post("/") { it.result(it.body() + it.body() + it.body()) }
        assertThat(http.post("/").body("Hi").asString().body).isEqualTo("HiHiHi")
    }

    @Test
    fun `can read payloads larger than max size by using inputstream`() = TestUtil.test(Javalin.create { it.http.maxRequestSize = 4L }) { app, http ->
        app.post("/body") { it.result(it.body()) }
        assertThat(http.post("/body").body("123456").asString().body).isEqualTo("Content Too Large")
        app.post("/stream") { it.result(it.req().inputStream.readBytes()) }
        assertThat(http.post("/stream").body("123456").asString().body).isEqualTo("123456")
    }

}
