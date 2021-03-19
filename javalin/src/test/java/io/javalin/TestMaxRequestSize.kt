package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestMaxRequestSize {

    @Test
    fun `max request size is set by default`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result(ctx.body()) }
        assertThat(http.post("/").body(ByteArray(1_000_000)).asString().status).isEqualTo(200)
        assertThat(http.post("/").body(ByteArray(1_000_001)).asString().status).isEqualTo(413)
    }

    @Test
    fun `user can configure max request size`() = TestUtil.test(Javalin.create { it.maxRequestSize = 4L }) { app, http ->
        app.post("/") { ctx -> ctx.result(ctx.body()) }
        assertThat(http.post("/").body(ByteArray(4)).asString().status).isEqualTo(200)
        assertThat(http.post("").body(ByteArray(5)).asString().status).isEqualTo(413)
    }

    @Test
    fun `body can be read multiple times`() = TestUtil.test(Javalin.create()) { app, http ->
        app.post("/") { ctx -> ctx.result(ctx.body() + ctx.body() + ctx.body()) }
        assertThat(http.post("/").body("Hi").asString().body).isEqualTo("HiHiHi")
    }

    @Test
    fun `can read payloads larger than max size by using inputstream`() = TestUtil.test(Javalin.create { it.maxRequestSize = 4L }) { app, http ->
        app.post("/body") { ctx -> ctx.result(ctx.body()) }
        assertThat(http.post("/body").body("123456").asString().body).isEqualTo("Payload too large")
        app.post("/stream") { ctx -> ctx.result(ctx.req.inputStream.readBytes()) }
        assertThat(http.post("/stream").body("123456").asString().body).isEqualTo("123456")
    }

}
