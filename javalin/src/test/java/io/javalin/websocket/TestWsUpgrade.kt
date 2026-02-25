/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.http.HttpStatus
import io.javalin.security.RouteRole
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

@Timeout(value = 2, unit = TimeUnit.SECONDS)
class TestWsUpgrade {

    private enum class WsRole : RouteRole { A }

    @Test
    fun `wsBeforeUpgrade and wsAfterUpgrade are invoked`() = TestUtil.test { app, http ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsBeforeUpgrade { log.add("before") }
        app.unsafe.routes.wsAfterUpgrade { log.add("after") }
        app.unsafe.routes.ws("/ws") {}
        http.wsUpgradeRequest("/ws")
        assertThat(log).containsExactly("before", "after")
    }

    @Test
    fun `wsBeforeUpgrade and after can modify the upgrade request`() = TestUtil.test { app, http ->
        app.unsafe.routes.wsBeforeUpgrade { it.header("X-Before", "before") }
        app.unsafe.routes.wsAfterUpgrade { it.header("X-After", "after") }
        app.unsafe.routes.ws("/ws") {}
        val response = http.wsUpgradeRequest("/ws")
        assertThat(response.headers.getFirst("X-Before")).isEqualTo("before")
        assertThat(response.headers.getFirst("X-After")).isEqualTo("after")
    }

    @Test
    fun `wsBeforeUpgrade can stop an upgrade request in progress`() = TestUtil.test { app, http ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsBeforeUpgrade { _ -> throw IllegalStateException("denied") }
        app.unsafe.routes.ws("/ws") { ws ->
            ws.onConnect { log.add("connected") }
        }
        val response = http.wsUpgradeRequest("/ws")
        assertThat(response.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.code)
        assertThat(log).isEmpty()
    }

    @Test
    fun `wsBeforeUpgrade exception pattern can be combined with a custom exception handler`() = TestUtil.test { app, http ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsBeforeUpgrade { throw IllegalStateException("denied") }
        app.unsafe.routes.exception(IllegalStateException::class.java) { _, ctx ->
            log.add("exception handled")
            ctx.status(HttpStatus.FORBIDDEN)
        }
        app.unsafe.routes.ws("/ws") {}
        val response = http.wsUpgradeRequest("/ws")
        assertThat(log).containsExactly("exception handled")
        assertThat(response.status).isEqualTo(HttpStatus.FORBIDDEN.code)
    }

    @Test
    fun `wsBeforeUpgrade does work with skipRemainingHandlers`() = TestUtil.test { app, http ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsBeforeUpgrade { it.status(HttpStatus.FORBIDDEN).skipRemainingHandlers() }
        app.unsafe.routes.ws("/ws") { ws ->
            ws.onConnect { log.add("connected") }
        }
        val client = WsTestClient(app, "/ws")
        client.connectAndDisconnect()
        val response = http.wsUpgradeRequest("/ws")
        assertThat(response.status).isEqualTo(HttpStatus.FORBIDDEN.code)
        assertThat(log).isEmpty()
    }

    @Test
    fun `wsBeforeUpgrade in full lifecycle`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsBeforeUpgrade { log.add("before-upgrade") }
        app.unsafe.routes.wsAfterUpgrade { log.add("after-upgrade") }
        app.unsafe.routes.ws("/ws") { ws ->
            ws.onConnect { log.add("connect") }
            ws.onMessage { log.add("msg") }
            ws.onClose { log.add("close") }
        }
        val client = WsTestClient(app, "/ws")
        client.connectSendAndDisconnect("test-message")
        assertThat(log).containsExactly("before-upgrade", "after-upgrade", "connect", "msg", "close")
    }

    @Test
    fun `routeRoles are available in wsBeforeUpgrade`() = TestUtil.test { app, http ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsBeforeUpgrade { log.add(it.routeRoles().toString()) }
        app.unsafe.routes.ws("/ws", {}, WsRole.A)
        http.wsUpgradeRequest("/ws")
        assertThat(log).containsExactly("[A]")
    }

    @Test
    fun `attributes set in wsBeforeUpgrade are available in onConnect`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsBeforeUpgrade { it.attribute("hello", "world") }
        app.unsafe.routes.ws("/ws") { ws ->
            ws.onConnect { log.add(it.attribute<String>("hello") ?: "missing") }
        }
        WsTestClient(app, "/ws").connectAndDisconnect()
        assertThat(log).containsExactly("world")
    }

    @Test
    fun `pathParams are available in wsBeforeUpgrade`() = TestUtil.test { app, http ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.wsBeforeUpgrade { log.add(it.pathParam("param")) }
        app.unsafe.routes.ws("/ws/{param}") {}
        http.wsUpgradeRequest("/ws/123")
        assertThat(log).containsExactly("123")
    }
}

