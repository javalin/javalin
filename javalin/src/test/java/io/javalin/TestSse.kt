package io.javalin

import io.javalin.http.sse.SseClient
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.*

class TestSse {

    private val event = "HI"
    private val data = "Hello, world!"
    private fun shortTimeoutServer() = Javalin.create().after { it.req.asyncContext.timeout = 10 }

    @Test
    fun `sending events works`() = TestUtil.test(shortTimeoutServer()) { app, http ->
        app.sse("/sse") { it.sendEvent(event, data) }
        val body = http.sse("/sse").get().body
        assertThat(body).contains("event: $event")
        assertThat(body).contains("data: $data")
    }

    @Test
    fun `sending events with ID works`() = TestUtil.test(shortTimeoutServer()) { app, http ->
        app.sse("/sse") { it.sendEvent(event, data, id = "SOME_ID") }
        val body = http.sse("/sse").get().body
        assertThat(body).contains("id: SOME_ID")
        assertThat(body).contains("event: $event")
        assertThat(body).contains("data: $data")
    }

    @Test
    fun `sending events to multiple clients works`() {
        TestUtil.test(shortTimeoutServer()) { app, http ->
            val eventSources: MutableList<SseClient> = ArrayList()
            app.sse("/sse") { sse ->
                eventSources.add(sse)
                sse.sendEvent(event, data + eventSources.size)
            }
            val bodyClient1 = http.sse("/sse").get().body
            val bodyClient2 = http.sse("/sse").get().body
            assertThat(bodyClient1).isNotEqualTo(bodyClient2)
            assertThat(eventSources[0]).isNotEqualTo(eventSources[1])
        }
    }

    @Test
    fun `all headers are correctly configured`() = TestUtil.test(shortTimeoutServer()) { app, http ->
        app.sse("/sse") { it.sendEvent(event, data) }
        val headers = http.sse("/sse").get().headers // Headers extends HashMap<String, List<String>>
        assertThat(headers.getFirst("Connection")).containsIgnoringCase("close")
        assertThat(headers.getFirst("Content-Type")).containsIgnoringCase("text/event-stream")
        assertThat(headers.getFirst("Content-Type")).containsIgnoringCase("charset=utf-8")
        assertThat(headers.getFirst("Cache-Control")).containsIgnoringCase("no-cache")
    }

    @Test
    fun `default http status is 200`() = TestUtil.test(shortTimeoutServer()) { app, http ->
        app.sse("/sse") { it.sendEvent(event, data) }
        val status = http.sse("/sse").get().status
        assertThat(status).isEqualTo(200)
    }

    @Test
    fun `getting queryParam in sse handler works`() = TestUtil.test(shortTimeoutServer()) { app, http ->
        app.sse("/sse") { it.sendEvent(event, it.ctx.queryParam("qp")!!) }
        val body = http.sse("/sse?qp=my-qp").get().body
        assertThat(body).contains("event: $event")
        assertThat(body).contains("data: " + "my-qp")
    }
    
}
