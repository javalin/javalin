/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.json.toJsonString
import io.javalin.testing.SerializableObject
import io.javalin.testing.fasterJacksonMapper
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.util.BufferUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class TestWsMessaging {

    @Test
    fun `messages can be broadcast to all connected clients`() = wsTest({
        val connected = ConcurrentHashMap.newKeySet<WsContext>()
        it.routes.ws("/chat") { ws ->
            ws.onConnect { ctx -> connected += ctx; record("connected") }
            ws.onMessage { ctx -> connected.forEach { client -> client.send(ctx.message()) } }
            ws.onClose { ctx -> connected -= ctx }
        }
    }) {
        val alice = connect("/chat")
        val bob = connect("/chat")
        server.expect("connected", "connected")
        alice.send("hi from alice")
        assertThat(alice.nextMessage()).isEqualTo("hi from alice")
        assertThat(bob.nextMessage()).isEqualTo("hi from alice")
        bob.send("hi from bob")
        assertThat(alice.nextMessage()).isEqualTo("hi from bob")
        assertThat(bob.nextMessage()).isEqualTo("hi from bob")
    }

    @Test
    fun `receive and send json messages`() = wsTest({
        it.jsonMapper(fasterJacksonMapper)
        it.routes.ws("/message") { ws ->
            ws.onMessage { ctx ->
                val receivedMessage = ctx.messageAsClass<SerializableObject>()
                receivedMessage.value1 = "updated"
                ctx.send(receivedMessage)
            }
        }
    }) {
        val client = connect("/message")
        client.send(fasterJacksonMapper.toJsonString(SerializableObject().apply { value1 = "test1"; value2 = "test2" }))
        assertThat(client.nextMessage()).contains(""""value1":"updated"""", """"value2":"test2"""")
    }

    @Test
    fun `binary messages`() = wsTest({
        it.routes.ws("/binary") { ws ->
            ws.onBinaryMessage { ctx -> ctx.send(ByteBuffer.wrap(BufferUtil.toArray(ctx.data()))) }
        }
    }) {
        val bytes1 = Random.nextBytes(4096)
        val bytes2 = Random.nextBytes(4096)
        val client = connect("/binary")
        client.send(bytes1)
        client.send(bytes2)
        assertThat(client.nextBinary()).isEqualTo(bytes1)
        assertThat(client.nextBinary()).isEqualTo(bytes2)
    }
}
