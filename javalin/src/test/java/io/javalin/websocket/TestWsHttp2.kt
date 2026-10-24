package io.javalin.websocket

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.websocket.api.Callback
import org.eclipse.jetty.websocket.api.Session
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

// The client always speaks first in these tests: Jetty's HTTP/2 websocket client can drop
// frames that the server sends while the client is still completing the upgrade.
@Timeout(value = 5, unit = TimeUnit.SECONDS)
class TestWsHttp2 {

    @Test
    fun `websocket works over http2 via direct jetty mapping`() = wsTest(http2 = true, config = {
        it.jetty.modifyServletContextHandler { context ->
            JettyWebSocketServletContainerInitializer.configure(context) { _, container ->
                container.addMapping("/ws/jetty") { _, _ ->
                    object : Session.Listener.AbstractAutoDemanding() {
                        override fun onWebSocketText(message: String) {
                            session.sendText("jetty echo: $message", Callback.NOOP)
                        }
                    }
                }
            }
        }
    }) {
        val client = connect("/ws/jetty")
        assertThat(client.httpVersion).isEqualTo(HttpVersion.HTTP_2)
        client.send("hello")
        assertThat(client.nextMessage()).isEqualTo("jetty echo: hello")
    }

    @Test
    fun `websocket works over http2 via javalin routes`() = wsTest(http2 = true, config = {
        it.routes.ws("/ws/javalin") { ws ->
            ws.onMessage { ctx -> ctx.send("javalin echo: ${ctx.message()}") }
        }
    }) {
        val client = connect("/ws/javalin")
        assertThat(client.httpVersion).isEqualTo(HttpVersion.HTTP_2)
        client.send("hello")
        assertThat(client.nextMessage()).isEqualTo("javalin echo: hello")
    }
}
