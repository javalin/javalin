package io.javalin

import io.javalin.testing.TestUtil
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class TestRequestCache {

    @Test
    fun `request caching request does not drain InputStream`() = TestUtil.test { app, http ->
        app.post("/cache-chunked-encoding") { ctx -> ctx.result(ctx.req.inputStream) }
        val body = ByteArray(10000).mapIndexed { i, _ -> (i % 256).toByte() }.toByteArray()
        val post = HttpPost(http.origin + "/cache-chunked-encoding").apply {
            entity = ByteArrayEntity(body).apply { isChunked = true }
        }
        val response = HttpClients.createDefault().execute(post)
        val result = EntityUtils.toByteArray(response.entity)
        assertThat(body).isEqualTo(result)
        response.close()
    }

    @Test
    fun `disabling request-caching works`() = TestUtil.test(Javalin.create { it.requestCacheSize = 0 }) { app, http ->
        app.post("/disabled-cache") { ctx ->
            if (ctx.req.inputStream.javaClass.simpleName == "CachedServletInputStream") {
                throw IllegalStateException("Cache should be disabled.")
            }
        }
        val response = http.post("/disabled-cache").body("test").asBinary()
        assertThat(response.status).isEqualTo(200)
    }
}
