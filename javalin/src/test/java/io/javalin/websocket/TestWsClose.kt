/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.websocket.WsClient.Close
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.websocket.api.StatusCode
import org.eclipse.jetty.websocket.core.CloseStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.util.concurrent.TimeUnit

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class TestWsClose {

    enum class CloseScenario(val close: (WsContext) -> Unit, val expected: Close) {
        NO_ARGS({ it.closeSession() }, Close(1000, null)),
        WS_CLOSE_STATUS({ it.closeSession(WsCloseStatus.GOING_AWAY) }, Close(1001, WsCloseStatus.GOING_AWAY.message())),
        WS_CLOSE_STATUS_AND_REASON({ it.closeSession(WsCloseStatus.POLICY_VIOLATION, "custom") }, Close(1008, "custom")),
        CODE_AND_REASON({ it.closeSession(1002, "CODE_AND_REASON") }, Close(1002, "CODE_AND_REASON")),
        JETTY_CLOSE_STATUS({ it.closeSession(CloseStatus(4000, "app-defined")) }, Close(4000, "app-defined")),
    }

    @ParameterizedTest
    @EnumSource(CloseScenario::class)
    fun `closeSession() closes with the given code and reason`(scenario: CloseScenario) = wsTest({
        it.routes.ws("/ws") { ws ->
            ws.onConnect(scenario.close)
            ws.onClose { ctx -> record("closed with ${ctx.status()}") }
        }
    }) {
        assertThat(connect("/ws").awaitClose()).isEqualTo(scenario.expected)
        server.expect("closed with ${scenario.expected.code}")
    }

    private fun cannotBeSent(code: Int) = "Close status code $code cannot be sent to the client (RFC 6455, Section 7.4)"

    @Test
    fun `closeSession() rejects close codes that cannot be sent`() {
        val nonTransmittableCodes = listOf(1004, 1005, 1006, 1015, 999, 5000)
        wsTest({
            it.routes.ws("/ws") { ws ->
                ws.onConnect { ctx ->
                    nonTransmittableCodes.forEach { code ->
                        runCatching { ctx.closeSession(code, "reason") }.onFailure { e -> record(e.message!!) }
                    }
                }
            }
        }) {
            connect("/ws")
            server.expect(*nonTransmittableCodes.map(::cannotBeSent).toTypedArray())
        }
    }

    @Test
    fun `closeSession() with a reserved close code closes the session with a server error`() = wsTest({
        it.routes.ws("/ws") { ws ->
            ws.onConnect { ctx -> ctx.closeSession(WsCloseStatus.RESERVED) }
        }
    }) {
        assertThat(connect("/ws").awaitClose()).isEqualTo(Close(StatusCode.SERVER_ERROR, cannotBeSent(1004)))
    }

    @Test
    fun `the client's close code and reason are available in onClose`() = wsTest({
        it.routes.ws("/ws") { ws ->
            ws.onClose { ctx -> record("status=${ctx.status()} closeStatus=${ctx.closeStatus().name} reason=${ctx.reason()}") }
        }
    }) {
        connect("/ws").close(1001, "going away")
        connect("/ws").close(4000, "app-defined")
        server.expectInAnyOrder(
            "status=1001 closeStatus=GOING_AWAY reason=going away",
            "status=4000 closeStatus=UNKNOWN reason=app-defined",
        )
    }

    @Test
    fun `stopping the server disconnects open sessions and runs onClose`() = wsTest({
        it.routes.ws("/ws") { ws ->
            ws.onClose { record("closed") }
        }
    }) {
        val client = connect("/ws")
        app.stop()
        client.awaitClose()
        server.expect("closed")
    }
}
