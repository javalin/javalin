/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import org.eclipse.jetty.util.BufferUtil
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

    @Test
    fun `a client ping is answered with a pong with the same payload`() = wsTest({
        it.routes.ws("/ws") { ws -> ws.onPing { ctx -> record("ping:${String(BufferUtil.toArray(ctx.data()))}") } }
    }) {
        val client = connect("/ws")
        client.sendPing("Hello World".toByteArray())
        assertThat(client.nextPong()).isEqualTo("Hello World".toByteArray())
        server.expect("ping:Hello World")
    }

    @Test
    fun `onPong receives the pong payload`() = wsTest({
        it.routes.ws("/ws") { ws -> ws.onPong { ctx -> record("pong:${String(BufferUtil.toArray(ctx.data()))}") } }
    }) {
        connect("/ws").sendPong("Hello".toByteArray())
        server.expect("pong:Hello")
    }

    @Test
    fun `pings are not processed while pongs can't be written`() = wsTest({
        it.routes.ws("/ws") { ws ->
            ws.onConnect { ctx ->
                ctx.send("stop reading") // the client holds reading when this arrives
                ctx.send(ByteBuffer.allocate(10 * 1024 * 1024)) // fills the TCP buffers, so later writes are queued
            }
            ws.onPing { record("ping") }
        }
    }) {
        val client = connect("/ws", holdReading = true)
        client.awaitReadingHeld()
        repeat(10) { client.sendPing() }
        server.expect("ping") // the first ping is processed, the next waits for its pong to be written
        server.expectNoMore(within = Duration.ofMillis(200))
        client.resumeReading()
        repeat(10) { client.nextPong() }
        server.expect(*Array(10) { "ping" })
    }
}
