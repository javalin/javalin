package io.javalin.websocket

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.ee10.websocket.server.config.JettyWebSocketServletContainerInitializer
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.websocket.api.Callback
import org.eclipse.jetty.websocket.api.Session
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class TestWsHttp2 {

    @Test
    fun `websocket works over http2 via direct jetty mapping`() = wsTest(http2 = true, config = {
        it.jetty.modifyServletContextHandler { context ->
            JettyWebSocketServletContainerInitializer.configure(context) { _, container ->
                container.addMapping("/ws/jetty") { _, _ ->
                    object : Session.Listener.AbstractAutoDemanding() {
                        override fun onWebSocketOpen(session: Session) {
                            session.sendText("hello from jetty", Callback.NOOP)
                        }
                    }
                }
            }
        }
    }) {
        val client = connect("/ws/jetty")
        assertThat(client.httpVersion).isEqualTo(HttpVersion.HTTP_2)
        assertThat(client.nextMessage()).isEqualTo("hello from jetty")
    }

    @Test
    fun `websocket works over http2 via javalin routes`() = wsTest(http2 = true, config = {
        it.routes.ws("/ws/javalin") { ws ->
            ws.onConnect { ctx -> ctx.send("hello from javalin") }
        }
    }) {
        val client = connect("/ws/javalin")
        assertThat(client.httpVersion).isEqualTo(HttpVersion.HTTP_2)
        assertThat(client.nextMessage()).isEqualTo("hello from javalin")
    }
}
