package io.javalin

import io.javalin.http.HttpCode
import io.javalin.http.HttpCode.*
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TestMaxRequestSize {

    @Test
    fun `max request size is set by default`() = TestUtil.test { app, http ->
        app.post("/") { it.result(it.body()) }
        assertThat(http.post("/").body(ByteArray(1_000_000)).asString().status).isEqualTo(OK.status)
        assertThat(http.post("/").body(ByteArray(1_000_001)).asString().status).isEqualTo(CONTENT_TOO_LARGE.status)
    }

    @Test
    fun `user can configure max request size`() = TestUtil.test(Javalin.create { it.http.maxRequestSize = 4L }) { app, http ->
        app.post("/") { it.result(it.body()) }
        assertThat(http.post("/").body(ByteArray(4)).asString().status).isEqualTo(OK.status)
        assertThat(http.post("").body(ByteArray(5)).asString().status).isEqualTo(CONTENT_TOO_LARGE.status)
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
