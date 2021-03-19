package io.javalin

import io.javalin.testing.TestUtil
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestMaxRequestSize {

    @Test
    fun `max request size is set by default`() = TestUtil.test { app, http ->
        app.post("/") { ctx -> ctx.result(ctx.req.inputStream) }
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

}
