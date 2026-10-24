/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.http.HttpStatus
import io.javalin.http.UnauthorizedResponse
import io.javalin.plugin.bundled.DevLoggingPlugin
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class TestWsLogging {

    @Test
    fun `web socket logging works`() = wsTest({
        it.requestLogger.ws { ws ->
            ws.onConnect { ctx -> record("${ctx.pathParam("param")} connected") }
            ws.onMessage { ctx -> record("${ctx.pathParam("param")} sent ${ctx.message()}") }
            ws.onClose { ctx -> record("${ctx.pathParam("param")} disconnected") }
        }
        it.routes.ws("/path/{param}") {}
    }) {
        connect("/path/0").apply { send("hi") }.close()
        connect("/path/1").close()
        server.expectInAnyOrder("0 connected", "0 sent hi", "0 disconnected", "1 connected", "1 disconnected")
    }

    @Test
    fun `web socket upgrade logging works`() = wsTest({
        it.requestLogger.ws { ws ->
            ws.onUpgrade { ctx, _ -> record("${ctx.path()} upgrade attempted (${ctx.status()})") }
        }
        it.routes.ws("/upgrade-test") {}
    }) {
        connect("/upgrade-test")
        assertThat(expectRejectedUpgrade("/non-existent-ws").status).isEqualTo(HttpStatus.NOT_FOUND.code)
        server.expectInAnyOrder(
            "/upgrade-test upgrade attempted (101 Switching Protocols)",
            "/non-existent-ws upgrade attempted (404 Not Found)",
        )
    }

    @Test
    fun `web socket upgrade logging works for wsBeforeUpgrade errors`() = wsTest({
        it.requestLogger.ws { ws ->
            ws.onUpgrade { ctx, _ -> record("${ctx.path()} upgrade attempted (${ctx.status()})") }
        }
        it.routes.wsBeforeUpgrade("/auth-ws") { throw UnauthorizedResponse() }
        it.routes.ws("/auth-ws") {}
    }) {
        assertThat(expectRejectedUpgrade("/auth-ws").status).isEqualTo(HttpStatus.UNAUTHORIZED.code)
        server.expect("/auth-ws upgrade attempted (401 Unauthorized)")
    }

    @Test
    fun `dev logging preserves clean web socket close for query parameters`() = wsTest({
        it.registerPlugin(DevLoggingPlugin())
        it.routes.ws("/path") { ws ->
            ws.onConnect { ctx ->
                record("queryString=${ctx.queryString()}")
                record("queryParam=${ctx.queryParam("test")}")
                record("queryParams=${ctx.queryParams("hi")}")
            }
            ws.onClose { ctx -> record("server disconnected (${ctx.status()})") }
        }
    }) {
        connect("/path?test=banana&hi=1&hi=2").close()
        server.expect(
            "queryString=test=banana&hi=1&hi=2",
            "queryParam=banana",
            "queryParams=[1, 2]",
            "server disconnected (1000)",
        )
    }
}
