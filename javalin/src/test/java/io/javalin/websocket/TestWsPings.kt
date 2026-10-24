/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.TimeUnit

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class TestWsPings {

    @Test
    fun `sendPing() sends a ping with the given payload`() = wsTest({
        it.routes.ws("/ws") { ws ->
            ws.onConnect { ctx -> ctx.sendPing(ByteBuffer.wrap(byteArrayOf(1, 2, 3))) }
        }
    }) {
        assertThat(connect("/ws").nextPing()).isEqualTo(byteArrayOf(1, 2, 3))
    }

    @Test
    fun `enableAutomaticPings() sends pings with the default payload`() = wsTest({
        it.routes.ws("/ws") { ws ->
            ws.onConnect { ctx -> ctx.enableAutomaticPings(10, TimeUnit.MILLISECONDS) }
        }
    }) {
        val client = connect("/ws")
        assertThat(client.nextPing()).isEqualTo(byteArrayOf(0, 0, 0))
        assertThat(client.nextPing()).isEqualTo(byteArrayOf(0, 0, 0))
    }

    @Test
    fun `disableAutomaticPings() stops the pings`() = wsTest({
        it.routes.ws("/ws") { ws ->
            ws.onConnect { ctx -> ctx.enableAutomaticPings(10, TimeUnit.MILLISECONDS) }
            ws.onMessage { ctx ->
                ctx.disableAutomaticPings()
                ctx.send("pings disabled")
            }
        }
    }) {
        val client = connect("/ws")
        client.nextPing()
        client.send("disable pings")
        assertThat(client.nextMessage()).isEqualTo("pings disabled")
        client.expectNoMorePings(within = Duration.ofMillis(100))
    }
}
