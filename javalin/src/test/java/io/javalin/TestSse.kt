package io.javalin

import io.javalin.http.HttpStatus.OK
import io.javalin.http.sse.SseClient
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import io.javalin.testing.httpCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TestSse {

    private val event = "HI"
    private val data = "Hello, world!"

    private fun SseClient.doAndClose(runnable: Runnable) = runnable.run().also { this.close() }

    @Test
    fun `sending events works`() = TestUtil.test { app, http ->
        app.sse("/sse") { it.doAndClose { it.sendEvent(event, data) } }
        val body = http.sse("/sse").get().body
        assertThat(body).contains("event: $event")
        assertThat(body).contains("data: $data")
    }

    @Test
    fun `sending input stream works`() = TestUtil.test { app, http ->
        app.sse("/sse") { it.doAndClose { it.sendEvent(event, "MY DATA".byteInputStream()) } }
        assertThat(http.sse("/sse").get().body).contains("data: MY DATA")
    }

    @Test
    fun `sending json works`() = TestUtil.test { app, http ->
        app.sse("/sse") { it.doAndClose { it.sendEvent(event, SerializableObject()) } }
        assertThat(http.sse("/sse").get().body).contains("""data: {"value1":"FirstValue","value2":"SecondValue"}""")
    }

    @Test
    fun `sending events with ID works`() = TestUtil.test { app, http ->
        app.sse("/sse") { it.doAndClose { it.sendEvent(event, data, id = "SOME_ID") } }
        val body = http.sse("/sse").get().body
        assertThat(body).contains("id: SOME_ID")
        assertThat(body).contains("event: $event")
        assertThat(body).contains("data: $data")
    }

    @Test
    fun `sending events to multiple clients works`() = TestUtil.test { app, http ->
        val eventSources: MutableList<SseClient> = ArrayList()
        app.sse("/sse") {
            eventSources.add(it)
            it.sendEvent(event, data + eventSources.size)
            it.close()
        }
        val bodyClient1 = http.sse("/sse").get().body
        val bodyClient2 = http.sse("/sse").get().body
        assertThat(bodyClient1).isNotEqualTo(bodyClient2)
        assertThat(eventSources[0]).isNotEqualTo(eventSources[1])
    }

    @Test
    fun `all headers are correctly configured`() = TestUtil.test { app, http ->
        app.sse("/sse") { it.doAndClose { it.sendEvent(event, data) } }
        val headers = http.sse("/sse").get().headers // Headers extends HashMap<String, List<String>>
        assertThat(headers.getFirst("Connection")).containsIgnoringCase("close")
        assertThat(headers.getFirst("Content-Type")).containsIgnoringCase("text/event-stream")
        assertThat(headers.getFirst("Content-Type")).containsIgnoringCase("charset=utf-8")
        assertThat(headers.getFirst("Cache-Control")).containsIgnoringCase("no-cache")
    }

    @Test
    fun `can check if SseClient has been terminated`() = TestUtil.test { app, http ->
        var terminated = false
        app.sse("/sse") {
            it.close()
            terminated = it.terminated()
        }
        http.sse("/sse").get().body
        assertThat(terminated).isTrue()
    }

    @Test
    fun `default http status is 200`() = TestUtil.test { app, http ->
        app.sse("/sse") { it.doAndClose { it.sendEvent(event, data) } }
        val status = http.sse("/sse").get().httpCode()
        assertThat(status).isEqualTo(OK)
    }

    @Test
    fun `getting queryParam in sse handler works`() = TestUtil.test { app, http ->
        app.sse("/sse") { it.doAndClose { it.sendEvent(event, it.ctx().queryParam("qp")!!) } }
        val body = http.sse("/sse?qp=my-qp").get().body
        assertThat(body).contains("event: $event")
        assertThat(body).contains("data: " + "my-qp")
    }

    @Test
    fun `sending Comment works`() = TestUtil.test { app, http ->
        app.sse("/sse") { it.doAndClose { it.sendComment("test comment works") } }
        val body = http.sse("/sse?qp=my-qp").get().body
        assertThat(body).isEqualTo(": test comment works\n")
    }

    @Test
    fun `sending empty Comment works`() = TestUtil.test { app, http ->
        app.sse("/sse") { it.doAndClose { it.sendComment("") } }
        val body = http.sse("/sse?qp=my-qp").get().body
        assertThat(body).isEqualTo(": \n")
    }

    @Test
    fun `sending multi line Comment works`() = TestUtil.test { app, http ->
        app.sse("/sse") { it.doAndClose { it.sendComment("a\nb") } }
        val body = http.sse("/sse?qp=my-qp").get().body
        assertThat(body).isEqualTo(": a\n: b\n")
    }

    @Test
    fun `sending multi line data works`() = TestUtil.test { app, http ->
        app.sse("/sse") { it.doAndClose { it.sendEvent("a\nb") } }
        val body = http.sse("/sse").get().body
        assertThat(body).isEqualTo("event: message\ndata: a\ndata: b\n\n")
    }

    @Test
    fun `sending async data is properly processed`() = TestUtil.test { app, http ->
        app.sse("/sse") {
            it.sendEvent("Sync event")
            it.ctx().async {
                Thread.sleep(100)
                it.sendEvent("Async event")
            }
        }

        val body = http.sse("/sse").get().body

        assertThat(body.trim()).isEqualTo(
            """
            event: message
            data: Sync event

            event: message
            data: Async event
            """.trimIndent().trim()
        )
    }

    @Test
    fun `user can freeze sse handler to leak sse client outside the handler`() = TestUtil.test { app, http ->
        val clients = mutableListOf<SseClient>()

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate({
            clients.forEach {
                it.sendComment("Emitted and closed!")
                it.close()
            }
        }, 50L, 50L, TimeUnit.MILLISECONDS)

        app.sse("/sse") { client ->
            clients.add(client)
            client.keepAlive()
        }

        assertThat(http.sse("/sse").get().body.trim()).isEqualTo(": Emitted and closed!")
    }

}
