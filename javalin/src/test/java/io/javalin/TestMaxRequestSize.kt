package io.javalin

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestMaxRequestSize {

    @Test
    fun `if max request size unset accept any content size`() = TestUtil.test { app, http ->
        app.post("/max-content-size") { ctx -> ctx.result(ctx.req.inputStream) }
        val httpResponse = http.post("/max-content-size").body(ByteArray(100000)).asString()
        assertThat(httpResponse.status).isEqualTo(200)
    }

    @Test
    fun `if content is bigger than max request size return 413`() = TestUtil.test(Javalin.create {
        it.maxRequestSize = 0L
    }) { app, http ->
        app.post("/max-content-size") { ctx -> ctx.result(ctx.req.inputStream) }
        val httpResponse = http.post("/max-content-size").body("body").asString()
        assertThat(httpResponse.status).isEqualTo(413)
    }
}
