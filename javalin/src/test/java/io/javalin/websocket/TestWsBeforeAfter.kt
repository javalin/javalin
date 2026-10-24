/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.http.UnauthorizedResponse
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

@Timeout(value = 2, unit = TimeUnit.SECONDS)
class TestWsBeforeAfter {

    @Test
    fun `before handlers work`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsBefore { ws ->
            ws.onConnect { log.add("before handler: onConnect") }
            ws.onMessage { log.add("before handler: onMessage") }
            ws.onBinaryMessage { log.add("before handler: onBinaryMessage") }
            ws.onPing { log.add("before handler: onPing") }
            ws.onPong { log.add("before handler: onPong") }
            ws.onClose { log.add("before handler: onClose") }
        }

        app.unsafe.routes.ws("/ws") { ws ->
            ws.onConnect { log.add("endpoint handler: onConnect") }
            ws.onMessage { log.add("endpoint handler: onMessage") }
            ws.onBinaryMessage { log.add("endpoint handler: onBinaryMessage") }
            ws.onPing { log.add("endpoint handler: onPing") }
            ws.onPong { log.add("endpoint handler: onPong") }
            ws.onClose { log.add("endpoint handler: onClose") }
        }
        WsTestClient(app, "/ws").connectSendAllMessageTypesAndDisconnect()
        assertThat(log).containsExactly(
            "before handler: onConnect", "endpoint handler: onConnect",
            "before handler: onMessage", "endpoint handler: onMessage",
            "before handler: onBinaryMessage", "endpoint handler: onBinaryMessage",
            "before handler: onPing", "endpoint handler: onPing",
            "before handler: onPong", "endpoint handler: onPong",
            "before handler: onClose", "endpoint handler: onClose"
        )
    }

    @Test
    fun `throw in wsBefore short circuits endpoint handler`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsBefore { it.onConnect { throw UnauthorizedResponse() } }
        app.unsafe.routes.ws("/ws") { it.onConnect { log.add("This should not be added") } }
        app.unsafe.routes.wsException(Exception::class.java) { e, _ -> log.add(e.message!!) }
        WsTestClient(app, "/ws").connectAndDisconnect()
        assertThat(log).contains("Unauthorized")
        assertThat(log).doesNotContain("This should not be added")
    }

    @Test
    fun `throw in wsAfter is covered by wsException`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsAfter { it.onConnect { throw UnauthorizedResponse() } }
        app.unsafe.routes.ws("/ws") { it.onConnect { log.add("This should be added") } }
        app.unsafe.routes.wsException(Exception::class.java) { e, _ -> log.add(e.message!!) }
        WsTestClient(app, "/ws").connectAndDisconnect()
        assertThat(log).contains("Unauthorized")
        assertThat(log).contains("This should be added")
    }

    @Test
    fun `wsAfter works for onConnect`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsAfter { it.onConnect { log.add("After!") } }
        app.unsafe.routes.ws("/ws") { it.onConnect { log.add("Endpoint!") } }
        WsTestClient(app, "/ws").connectAndDisconnect()
        assertThat(log).containsExactly("Endpoint!", "After!")
    }

    @Test
    fun `wsBefore with path works`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsBefore("/ws/*") { it.onConnect { log.add("Before!") } }
        app.unsafe.routes.ws("/ws/test") { it.onConnect { log.add("Endpoint!") } }
        WsTestClient(app, "/ws/test").connectAndDisconnect()
        assertThat(log).containsExactly("Before!", "Endpoint!")
    }

    @Test
    fun `multiple before and after handlers can be called`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsBefore { it.onConnect { log.add("Before 1") } }
        app.unsafe.routes.wsBefore("/ws/*") { it.onConnect { log.add("Before 2") } }
        app.unsafe.routes.wsAfter { it.onConnect { log.add("After 1") } }
        app.unsafe.routes.wsAfter("/ws/*") { it.onConnect { log.add("After 2") } }
        app.unsafe.routes.ws("/ws/test") { it.onConnect { log.add("Endpoint") } }
        WsTestClient(app, "/ws/test").connectAndDisconnect()
        assertThat(log).containsExactly("Before 1", "Before 2", "Endpoint", "After 1", "After 2")
    }

    @Test
    fun `after handlers work`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/ws") { ws ->
            ws.onConnect { log.add("endpoint handler: onConnect") }
            ws.onMessage { log.add("endpoint handler: onMessage") }
            ws.onBinaryMessage { log.add("endpoint handler: onBinaryMessage") }
            ws.onPing { log.add("endpoint handler: onPing") }
            ws.onPong { log.add("endpoint handler: onPong") }
            ws.onClose { log.add("endpoint handler: onClose") }
        }
        app.unsafe.routes.wsAfter { ws ->
            ws.onConnect { log.add("after handler: onConnect") }
            ws.onMessage { log.add("after handler: onMessage") }
            ws.onBinaryMessage { log.add("after handler: onBinaryMessage") }
            ws.onPing { log.add("after handler: onPing") }
            ws.onPong { log.add("after handler: onPong") }
            ws.onClose { log.add("after handler: onClose") }
        }
        WsTestClient(app, "/ws").connectSendAllMessageTypesAndDisconnect()
        assertThat(log).containsExactly(
            "endpoint handler: onConnect", "after handler: onConnect",
            "endpoint handler: onMessage", "after handler: onMessage",
            "endpoint handler: onBinaryMessage", "after handler: onBinaryMessage",
            "endpoint handler: onPing", "after handler: onPing",
            "endpoint handler: onPong", "after handler: onPong",
            "endpoint handler: onClose", "after handler: onClose"
        )
    }
}

