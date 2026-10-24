/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.apibuilder.ApiBuilder.path
import io.javalin.apibuilder.ApiBuilder.ws
import io.javalin.apibuilder.ApiBuilder.wsAfter
import io.javalin.apibuilder.ApiBuilder.wsBefore
import io.javalin.http.UnauthorizedResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class TestWsBeforeAfter {

    @Test
    fun `before handlers work`() = wsTest({
        it.routes.wsBefore(recorder("before"))
        it.routes.ws("/ws", recorder("endpoint"))
    }) {
        connect("/ws").apply { send("test") }.close()
        server.expect(
            "before:connect", "endpoint:connect",
            "before:message:test", "endpoint:message:test",
            "before:close:1000", "endpoint:close:1000",
        )
    }

    @Test
    fun `after handlers work`() = wsTest({
        it.routes.ws("/ws", recorder("endpoint"))
        it.routes.wsAfter(recorder("after"))
    }) {
        connect("/ws").apply { send("test") }.close()
        server.expect(
            "endpoint:connect", "after:connect",
            "endpoint:message:test", "after:message:test",
            "endpoint:close:1000", "after:close:1000",
        )
    }

    @Test
    fun `path-scoped before and after handlers only run for matching paths`() = wsTest({
        it.routes.wsBefore { ws -> ws.onConnect { record("before all") } }
        it.routes.wsBefore("/ws/*") { ws -> ws.onConnect { record("before /ws/*") } }
        it.routes.wsAfter { ws -> ws.onConnect { record("after all") } }
        it.routes.wsAfter("/ws/*") { ws -> ws.onConnect { record("after /ws/*") } }
        it.routes.ws("/ws/test") { ws -> ws.onConnect { record("endpoint /ws/test") } }
        it.routes.ws("/other") { ws -> ws.onConnect { record("endpoint /other") } }
    }) {
        connect("/ws/test")
        server.expect("before all", "before /ws/*", "endpoint /ws/test", "after all", "after /ws/*")
        connect("/other")
        server.expect(
            "before all", "before /ws/*", "endpoint /ws/test", "after all", "after /ws/*",
            "before all", "endpoint /other", "after all",
        )
    }

    @Test
    fun `ApiBuilder path prefixes apply to ws, wsBefore and wsAfter`() = wsTest({
        it.routes.apiBuilder {
            path("/api") {
                wsBefore("/*") { ws -> ws.onConnect { record("before") } }
                ws("/chat") { ws -> ws.onConnect { record("endpoint") } }
                wsAfter("/*") { ws -> ws.onConnect { record("after") } }
            }
        }
    }) {
        connect("/api/chat")
        server.expect("before", "endpoint", "after")
    }

    @Test
    fun `throw in wsBefore short circuits endpoint handler`() = wsTest({
        it.routes.wsBefore { ws -> ws.onConnect { throw UnauthorizedResponse() } }
        it.routes.ws("/ws") { ws -> ws.onConnect { record("endpoint:connect") } }
        it.routes.wsException(Exception::class.java) { e, _ -> record("exception:${e.message}") }
    }) {
        connect("/ws")
        server.expect("exception:Unauthorized")
    }

    @Test
    fun `throw in wsAfter is covered by wsException`() = wsTest({
        it.routes.wsAfter { ws -> ws.onConnect { throw UnauthorizedResponse() } }
        it.routes.ws("/ws") { ws -> ws.onConnect { record("endpoint:connect") } }
        it.routes.wsException(Exception::class.java) { e, _ -> record("exception:${e.message}") }
    }) {
        connect("/ws")
        server.expect("endpoint:connect", "exception:Unauthorized")
    }
}
