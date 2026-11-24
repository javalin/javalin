/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.testing.TypedException
import io.javalin.websocket.WsClient.Close
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.websocket.api.StatusCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class TestWsException {

    @Test
    fun `unmapped exceptions are caught by default handler`() = wsTest({
        it.routes.ws("/ws") { ws -> ws.onConnect { throw Exception("EX") } }
    }) {
        assertThat(connect("/ws").awaitClose()).isEqualTo(Close(StatusCode.SERVER_ERROR, "EX"))
    }

    @Test
    fun `mapped exceptions are handled`() = wsTest({
        it.routes.ws("/ws") { ws -> ws.onConnect { throw Exception() } }
        it.routes.wsException(Exception::class.java) { _, _ -> record("Exception handler called") }
    }) {
        connect("/ws")
        server.expect("Exception handler called")
    }

    @Test
    fun `most specific exception handler handles exception`() = wsTest({
        it.routes.ws("/ws") { ws -> ws.onConnect { throw TypedException() } }
        it.routes.wsException(Exception::class.java) { _, _ -> record("Exception handler called") }
        it.routes.wsException(TypedException::class.java) { _, _ -> record("TypedException handler called") }
    }) {
        connect("/ws")
        server.expect("TypedException handler called")
    }

    @Test
    fun `exceptions in onMessage are handled and the connection stays open`() = wsTest({
        it.routes.ws("/ws") { ws ->
            ws.onMessage { ctx ->
                if (ctx.message() == "boom") throw IllegalStateException("boom")
                ctx.send("echo:${ctx.message()}")
            }
        }
        it.routes.wsException(IllegalStateException::class.java) { e, ctx -> ctx.send("handled:${e.message}") }
    }) {
        val client = connect("/ws")
        client.send("boom")
        assertThat(client.nextMessage()).isEqualTo("handled:boom")
        client.send("hello")
        assertThat(client.nextMessage()).isEqualTo("echo:hello")
    }
}
