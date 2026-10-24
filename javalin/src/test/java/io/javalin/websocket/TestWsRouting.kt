/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.config.JavalinConfig
import io.javalin.http.HttpStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class TestWsRouting {

    private val paramRoutes: WsTest.(JavalinConfig) -> Unit = {
        it.routes.ws("/params/{1}") { ws ->
            ws.onConnect { ctx -> record(ctx.pathParam("1")) }
        }
        it.routes.ws("/params/{1}/test/{2}/{3}") { ws ->
            ws.onConnect { ctx -> record("${ctx.pathParam("1")} ${ctx.pathParam("2")} ${ctx.pathParam("3")}") }
        }
        it.routes.ws("/*") { ws ->
            ws.onConnect { record("catchall") }
        }
    }

    @Test
    fun `routing and pathParams work without context path`() = wsTest(paramRoutes) {
        connect("/params/one")
        connect("/params/%E2%99%94")
        connect("/params/another/test/long/path")
        server.expectInAnyOrder("one", "♔", "another long path")
    }

    @Test
    fun `routing and pathParams work with context path`() = wsTest({
        it.router.contextPath = "/websocket"
        paramRoutes(it)
    }) {
        connect("/websocket/params/one")
        connect("/websocket/params/%E2%99%94")
        connect("/websocket/params/another/test/long/path")
        server.expectInAnyOrder("one", "♔", "another long path")
    }

    @Test
    fun `context-path vs no context-path app`() = wsTest({
        it.routes.ws("/p/{id}") { ws -> ws.onConnect { ctx -> record(ctx.pathParam("id")) } }
    }) {
        val noContextPathApp = this
        wsTest({
            it.router.contextPath = "/websocket"
            it.routes.ws("/p/{id}") { ws -> ws.onConnect { ctx -> record(ctx.pathParam("id")) } }
        }) {
            noContextPathApp.connect("/p/ncpa")
            connect("/websocket/p/cpa")
            noContextPathApp.server.expect("ncpa")
            server.expect("cpa")
        }
    }

    @Test
    fun `websocket 404 works`() = wsTest {
        assertThat(expectRejectedUpgrade("/invalid-path").status).isEqualTo(HttpStatus.NOT_FOUND.code)
    }

    @Test
    fun `path matching is case sensitive`() = wsTest({
        it.routes.ws("/pAtH/{param}") { ws ->
            ws.onConnect { ctx -> record(ctx.pathParam("param")) }
        }
    }) {
        assertThat(expectRejectedUpgrade("/PaTh/my-param").status).isEqualTo(HttpStatus.NOT_FOUND.code)
        connect("/pAtH/My-PaRaM")
        server.expect("My-PaRaM")
    }
}
