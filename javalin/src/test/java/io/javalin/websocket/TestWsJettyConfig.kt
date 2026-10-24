/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.websocket.api.StatusCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Duration
import java.util.concurrent.TimeUnit

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class TestWsJettyConfig {

    @Test
    fun `max text message size can be configured`() = wsTest({
        it.jetty.modifyWebSocketServletFactory { wsFactory -> wsFactory.maxTextMessageSize = 1 }
        it.routes.ws("/ws") { ws ->
            ws.onError { ctx -> record("${ctx.error()!!::class.simpleName}: ${ctx.error()!!.message}") }
        }
    }) {
        val client = connect("/ws")
        client.send("This text is far too long.")
        server.expect("MessageTooLargeException: Text message too large: 26 > 1")
        assertThat(client.awaitClose().code).isEqualTo(StatusCode.MESSAGE_TOO_LARGE)
    }

    @Test
    fun `idle timeout can be configured`() = wsTest({
        it.jetty.modifyWebSocketServletFactory { wsFactory -> wsFactory.idleTimeout = Duration.ofMillis(10) }
        it.routes.ws("/ws") { ws ->
            ws.onClose { ctx -> record("closed with ${ctx.status()}") }
        }
    }) {
        assertThat(connect("/ws").awaitClose().code).isEqualTo(StatusCode.SHUTDOWN)
        server.expect("closed with ${StatusCode.SHUTDOWN}")
    }
}
