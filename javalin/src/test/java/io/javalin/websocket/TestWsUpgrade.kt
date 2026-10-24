/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.http.Header
import io.javalin.http.HttpStatus
import io.javalin.http.UnauthorizedResponse
import io.javalin.security.RouteRole
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class TestWsUpgrade {

    private enum class WsRole : RouteRole { A }

    @Test
    fun `wsBeforeUpgrade in full lifecycle`() = wsTest({
        it.routes.wsBeforeUpgrade { record("before-upgrade") }
        it.routes.wsAfterUpgrade { record("after-upgrade") }
        it.routes.ws("/ws", recorder("endpoint"))
    }) {
        connect("/ws").apply { send("test-message") }.close()
        server.expect("before-upgrade", "after-upgrade", "endpoint:connect", "endpoint:message:test-message", "endpoint:close:1000")
    }

    @Test
    fun `path-scoped upgrade handlers only run for matching paths`() = wsTest({
        it.routes.wsBeforeUpgrade("/secure/*") { ctx -> record("before ${ctx.path()}") }
        it.routes.wsAfterUpgrade("/secure/*") { ctx -> record("after ${ctx.path()}") }
        it.routes.ws("/public/ws") {}
        it.routes.ws("/secure/ws") {}
    }) {
        connect("/public/ws")
        connect("/secure/ws")
        server.expect("before /secure/ws", "after /secure/ws")
    }

    @Test
    fun `wsBeforeUpgrade can modify the upgrade response`() = wsTest({
        it.routes.wsBeforeUpgrade { ctx -> ctx.header("X-Before", "before") }
        it.routes.ws("/ws") {}
    }) {
        assertThat(connect("/ws").upgradeResponse.getHeader("X-Before")).isEqualTo("before")
    }

    @Test
    fun `wsBeforeUpgrade can stop an upgrade request in progress`() = wsTest({
        it.routes.wsBeforeUpgrade { throw IllegalStateException("denied") }
        it.routes.ws("/ws") { ws -> ws.onConnect { record("connected") } }
    }) {
        assertThat(expectRejectedUpgrade("/ws").status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.code)
        server.expectNothing()
    }

    @Test
    fun `an HttpResponseException in wsBeforeUpgrade rejects the upgrade with its status`() = wsTest({
        it.routes.wsBeforeUpgrade { throw UnauthorizedResponse() }
        it.routes.ws("/ws") { ws -> ws.onConnect { record("connected") } }
    }) {
        assertThat(expectRejectedUpgrade("/ws").status).isEqualTo(HttpStatus.UNAUTHORIZED.code)
        server.expectNothing()
    }

    @Test
    fun `wsBeforeUpgrade exception pattern can be combined with a custom exception handler`() = wsTest({
        it.routes.wsBeforeUpgrade { throw IllegalStateException("denied") }
        it.routes.exception(IllegalStateException::class.java) { _, ctx ->
            record("exception handled")
            ctx.status(HttpStatus.FORBIDDEN)
        }
        it.routes.ws("/ws") {}
    }) {
        assertThat(expectRejectedUpgrade("/ws").status).isEqualTo(HttpStatus.FORBIDDEN.code)
        server.expect("exception handled")
    }

    @Test
    fun `wsBeforeUpgrade does work with skipRemainingHandlers`() = wsTest({
        it.routes.wsBeforeUpgrade { ctx -> ctx.status(HttpStatus.FORBIDDEN).skipRemainingHandlers() }
        it.routes.ws("/ws") { ws -> ws.onConnect { record("connected") } }
    }) {
        assertThat(expectRejectedUpgrade("/ws").status).isEqualTo(HttpStatus.FORBIDDEN.code)
        server.expectNothing()
    }

    @Test
    fun `routeRoles are available in wsBeforeUpgrade`() = wsTest({
        it.routes.wsBeforeUpgrade { ctx -> record(ctx.routeRoles().toString()) }
        it.routes.ws("/ws", {}, WsRole.A)
    }) {
        connect("/ws")
        server.expect("[A]")
    }

    @Test
    fun `attributes set in wsBeforeUpgrade are available in onConnect`() = wsTest({
        it.routes.wsBeforeUpgrade { ctx -> ctx.attribute("hello", "world") }
        it.routes.ws("/ws") { ws ->
            ws.onConnect { ctx -> record("attribute=${ctx.attribute<String>("hello")}") }
        }
    }) {
        connect("/ws")
        server.expect("attribute=world")
    }

    @Test
    fun `pathParams are available in wsBeforeUpgrade`() = wsTest({
        it.routes.wsBeforeUpgrade { ctx -> record(ctx.pathParam("param")) }
        it.routes.ws("/ws/{param}") {}
    }) {
        connect("/ws/123")
        server.expect("123")
    }

    @Test
    fun `the first offered subprotocol is accepted`() = wsTest({
        it.routes.ws("/ws") {}
    }) {
        assertThat(connect("/ws", subProtocols = listOf("mqtt", "wamp")).upgradeResponse.acceptedSubProtocol).isEqualTo("mqtt")
        assertThat(connect("/ws").upgradeResponse.acceptedSubProtocol).isNull()
    }

    @Test
    fun `upgrade requests don't create http sessions`() = wsTest({
        it.routes.ws("/ws") {}
    }) {
        assertThat(connect("/ws").upgradeResponse.getHeaders(Header.SET_COOKIE)).isEmpty()
        assertThat(expectRejectedUpgrade("/no-such-ws").headers.getValuesList(Header.SET_COOKIE)).isEmpty()
    }
}
