/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.Javalin
import io.javalin.http.Header
import io.javalin.http.UnauthorizedResponse
import io.javalin.plugin.bundled.DevLoggingPlugin
import io.javalin.testing.TestUtil
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

@Timeout(value = 2, unit = TimeUnit.SECONDS)
class TestWsLogging {

    @Test
    fun `web socket logging works`() {
        val log = ConcurrentLinkedQueue<String>()
        TestUtil.test(Javalin.create {
            it.requestLogger.ws { ws ->
                ws.onConnect { ctx -> log.add("${ctx.pathParam("param")} connected") }
                ws.onClose { ctx -> log.add("${ctx.pathParam("param")} disconnected") }
            }
        }) { app, _ ->
            app.unsafe.routes.ws("/path/{param}") {}
            WsTestClient(app, "/path/0").connectAndDisconnect()
            WsTestClient(app, "/path/1").connectAndDisconnect()
            awaitCondition(condition = { log.size >= 4 })
            assertThat(log).containsExactlyInAnyOrder(
                "0 connected",
                "1 connected",
                "0 disconnected",
                "1 disconnected"
            )
        }
    }

    @Test
    fun `web socket upgrade logging works`() {
        val log = ConcurrentLinkedQueue<String>()
        TestUtil.test(Javalin.create {
            it.requestLogger.ws { ws ->
                ws.onUpgrade { ctx, _ -> log.add("${ctx.path()} upgrade attempted (${ctx.status()})") }
            }
        }) { app, _ ->
            app.unsafe.routes.ws("/upgrade-test") {}
            // Test successful upgrade
            WsTestClient(app, "/upgrade-test").connectAndDisconnect()
            // Test failed upgrade (404)
            val response = Unirest.get("http://localhost:${app.port()}/non-existent-ws")
                .header(Header.SEC_WEBSOCKET_KEY, "test-key")
                .header(Header.UPGRADE, "websocket")
                .header(Header.CONNECTION, "upgrade")
                .asString()
            assertThat(response.status).isEqualTo(404)

            // Verify that both successful and failed upgrades are logged
            assertThat(log).containsExactlyInAnyOrder(
                "/upgrade-test upgrade attempted (101 Switching Protocols)",
                "/non-existent-ws upgrade attempted (404 Not Found)"
            )
        }
    }

    @Test
    fun `web socket upgrade logging works for wsBeforeUpgrade errors`() {
        val log = ConcurrentLinkedQueue<String>()
        TestUtil.test(Javalin.create {
            it.requestLogger.ws { ws ->
                ws.onUpgrade { ctx, _ -> log.add("${ctx.path()} upgrade attempted (${ctx.status()})") }
            }
        }) { app, _ ->
            app.unsafe.routes.wsBeforeUpgrade("/auth-ws") { throw UnauthorizedResponse() }
            app.unsafe.routes.ws("/auth-ws") {}

            // Test failed upgrade due to authentication
            val response = Unirest.get("http://localhost:${app.port()}/auth-ws")
                .header(Header.SEC_WEBSOCKET_KEY, "test-key")
                .header(Header.UPGRADE, "websocket")
                .header(Header.CONNECTION, "upgrade")
                .asString()
            assertThat(response.status).isEqualTo(401)

            // Verify that the failed upgrade due to authentication error is logged
            assertThat(log).containsExactly("/auth-ws upgrade attempted (401 Unauthorized)")
        }
    }

    @Test
    fun `dev logging works for web sockets`() = TestUtil.test(Javalin.create { it.registerPlugin(DevLoggingPlugin()) }) { app, _ ->
        app.unsafe.routes.ws("/path/{param}") {}
        WsTestClient(app, "/path/0").connectAndDisconnect()
        WsTestClient(app, "/path/1?test=banana&hi=1&hi=2").connectAndDisconnect()
        // DevLoggingPlugin logs to console, not to our log - just verify no exceptions
    }
}

