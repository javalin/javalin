/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.Javalin
import io.javalin.testing.TestUtil
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

@Timeout(value = 2, unit = TimeUnit.SECONDS)
class TestWsRouting {

    @Test
    fun `routing and pathParams work without context path`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/params/{1}") { ws ->
            ws.onConnect { ctx -> log.add(ctx.pathParam("1")) }
        }
        app.unsafe.routes.ws("/params/{1}/test/{2}/{3}") { ws ->
            ws.onConnect { ctx -> log.add("${ctx.pathParam("1")} ${ctx.pathParam("2")} ${ctx.pathParam("3")}") }
        }
        app.unsafe.routes.ws("/*") { ws ->
            ws.onConnect { log.add("catchall") }
        }
        WsTestClient(app, "/params/one").connectAndDisconnect()
        WsTestClient(app, "/params/%E2%99%94").connectAndDisconnect()
        WsTestClient(app, "/params/another/test/long/path").connectAndDisconnect()
        assertThat(log).containsExactlyInAnyOrder("one", "♔", "another long path")
        assertThat(log).doesNotContain("catchall")
    }

    @Test
    fun `routing and pathParams work with context path`() = TestUtil.test(contextPathJavalin()) { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/params/{1}") { ws ->
            ws.onConnect { ctx -> log.add(ctx.pathParam("1")) }
        }
        app.unsafe.routes.ws("/params/{1}/test/{2}/{3}") { ws ->
            ws.onConnect { ctx -> log.add("${ctx.pathParam("1")} ${ctx.pathParam("2")} ${ctx.pathParam("3")}") }
        }
        app.unsafe.routes.ws("/*") { ws ->
            ws.onConnect { log.add("catchall") }
        }
        WsTestClient(app, "/websocket/params/one").connectAndDisconnect()
        WsTestClient(app, "/websocket/params/%E2%99%94").connectAndDisconnect()
        WsTestClient(app, "/websocket/params/another/test/long/path").connectAndDisconnect()
        assertThat(log).containsExactlyInAnyOrder("one", "♔", "another long path")
        assertThat(log).doesNotContain("catchall")
    }

    @Test
    fun `context-path vs no context-path app`() {
        val log1 = ConcurrentLinkedQueue<String>()
        val log2 = ConcurrentLinkedQueue<String>()
        val noContextPathApp = Javalin.create().apply {
            this.unsafe.routes.ws("/p/{id}") { it.onConnect { log1.add(it.pathParam("id")) } }
        }.start(0)
        val contextPathApp = Javalin.create { it.router.contextPath = "/websocket" }.apply {
            this.unsafe.routes.ws("/p/{id}") { it.onConnect { log2.add(it.pathParam("id")) } }
        }.start(0)
        try {
            WsTestClient(noContextPathApp, "/p/ncpa").connectAndDisconnect()
            WsTestClient(contextPathApp, "/websocket/p/cpa").connectAndDisconnect()
            assertThat(log1).containsExactly("ncpa")
            assertThat(log2).containsExactly("cpa")
        } finally {
            noContextPathApp.stop()
            contextPathApp.stop()
        }
    }

    @Test
    fun `websocket 404 works`() = TestUtil.test { app, _ ->
        val response = Unirest.get("http://localhost:" + app.port() + "/invalid-path")
            .header("Connection", "Upgrade")
            .header("Upgrade", "websocket")
            .header("Host", "localhost:" + app.port())
            .header("Sec-WebSocket-Key", "SGVsbG8sIHdvcmxkIQ==")
            .header("Sec-WebSocket-Version", "13")
            .asString()
        assertThat(response.body).containsSequence("WebSocket handler not found")
    }

    @Test
    fun `path matching is case sensitive`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/pAtH/{param}") { ws ->
            ws.onConnect { ctx -> log.add(ctx.pathParam("param")) }
        }
        // Request with different case should NOT match
        WsTestClient(app, "/PaTh/my-param").connectAndDisconnect()
        assertThat(log).isEmpty()
        // Request with exact case should match, and path param preserves case
        WsTestClient(app, "/pAtH/My-PaRaM").connectAndDisconnect()
        assertThat(log).containsExactly("My-PaRaM")
    }
}

