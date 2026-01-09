/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.testing.TestUtil
import io.javalin.testing.TypedException
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.websocket.api.StatusCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

@Timeout(value = 2, unit = TimeUnit.SECONDS)
class TestWsException {

    @Test
    fun `unmapped exceptions are caught by default handler`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/ws") { it.onConnect { throw Exception("EX") } }
        val client = object : WsTestClient(app, "/ws") {
            override fun onClose(status: Int, message: String, byRemote: Boolean) {
                log.add("Status code: $status")
                log.add("Reason: $message")
            }
        }
        awaitCondition(condition = { client.isClosed }) { client.connect() }
        assertThat(log).containsExactly(
            "Status code: ${StatusCode.SERVER_ERROR}",
            "Reason: EX"
        )
    }

    @Test
    fun `mapped exceptions are handled`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/ws") { it.onConnect { throw Exception() } }
        app.unsafe.routes.wsException(Exception::class.java) { _, _ -> log.add("Exception handler called") }
        WsTestClient(app, "/ws").connectAndDisconnect()
        assertThat(log).containsExactly("Exception handler called")
    }

    @Test
    fun `most specific exception handler handles exception`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/ws") { it.onConnect { throw TypedException() } }
        app.unsafe.routes.wsException(Exception::class.java) { _, _ -> log.add("Exception handler called") }
        app.unsafe.routes.wsException(TypedException::class.java) { _, _ -> log.add("TypedException handler called") }
        WsTestClient(app, "/ws").connectAndDisconnect()
        assertThat(log).containsExactly("TypedException handler called")
    }
}

