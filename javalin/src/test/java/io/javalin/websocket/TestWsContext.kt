/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.http.Header
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

@Timeout(value = 5, unit = TimeUnit.SECONDS)
class TestWsContext {

    @Test
    fun `each connection receives a unique id`() = wsTest({
        it.routes.ws("/ws") { ws ->
            ws.onConnect { ctx -> record(ctx.sessionId()) }
            ws.onClose { ctx -> record(ctx.sessionId()) }
        }
    }) {
        connect("/ws").close()
        connect("/ws").close()
        connect("/ws").close()
        // every id is recorded once on connect and once on close
        val idCounts = server.await(6).groupingBy { it }.eachCount()
        assertThat(idCounts).hasSize(3)
        assertThat(idCounts.values).containsOnly(2)
    }

    @Test
    fun `headers and host are available in context`() = wsTest({
        it.routes.ws("/websocket") { ws ->
            ws.onConnect { ctx ->
                record("header=${ctx.header("Test")}")
                record("headerMap=${ctx.headerMap()["Test"]}")
            }
            ws.onClose { ctx -> record("host=${ctx.host()}") }
        }
    }) {
        connect("/websocket", headers = mapOf("Test" to "HeaderParameter")).close()
        server.expect("header=HeaderParameter", "headerMap=HeaderParameter", "host=localhost:${app.port()}")
    }

    @Test
    fun `extracting path information works`() = wsTest({
        it.routes.ws("/websocket/{channel}") { ws ->
            ws.onConnect { ctx ->
                record("matchedPath=${ctx.endpoint().path}")
                record("pathParam=${ctx.pathParam("channel")}")
                record("pathParamMap=${ctx.pathParamMap()}")
                record("queryParam=${ctx.queryParam("qp")}")
                record("queryParams=${ctx.queryParams("qps")}")
            }
        }
    }) {
        connect("/websocket/channel-one?qp=just-a-qp&qps=1&qps=2")
        server.expect(
            "matchedPath=/websocket/{channel}",
            "pathParam=channel-one",
            "pathParamMap={channel=channel-one}",
            "queryParam=just-a-qp",
            "queryParams=[1, 2]",
        )
    }

    @Test
    fun `typed path and query params are available`() = wsTest({
        it.routes.ws("/users/{id}") { ws ->
            ws.onConnect { ctx ->
                record("id+1=${ctx.pathParamAsClass<Int>("id").get() + 1}")
                record("page+1=${ctx.queryParamAsClass<Int>("page").get() + 1}")
            }
        }
    }) {
        connect("/users/41?page=1")
        server.expect("id+1=42", "page+1=2")
    }

    @Test
    fun `extracting path information works in all handlers`() = wsTest({
        it.routes.ws("/context-life") { ws ->
            ws.onConnect { ctx -> record("${ctx.queryParam("qp")}-connect") }
            ws.onMessage { ctx -> record("${ctx.queryParam("qp")}-message") }
            ws.onClose { ctx -> record("${ctx.queryParam("qp")}-close") }
        }
    }) {
        connect("/context-life?qp=great").apply { send("not-important") }.close()
        server.expect("great-connect", "great-message", "great-close")
    }

    @Test
    fun `set attributes works`() = wsTest({
        it.routes.ws("/attributes") { ws ->
            ws.onConnect { ctx -> ctx.attribute("test", "Success") }
            ws.onClose { ctx ->
                record("attribute=${ctx.attribute<String>("test")}")
                record("attributeMap=${ctx.attributeMap()["test"]}")
            }
        }
    }) {
        connect("/attributes").close()
        server.expect("attribute=Success", "attributeMap=Success")
    }

    @Test
    fun `getting session attributes works`() = wsTest({
        it.routes.get("/") { ctx -> ctx.sessionAttribute("session-key", "session-value") }
        it.routes.ws("/") { ws ->
            ws.onConnect { ctx ->
                record("sessionAttribute=${ctx.sessionAttribute<String>("session-key")}")
                record("sessionAttributeMapSize=${ctx.sessionAttributeMap().size}")
            }
        }
    }) {
        val sessionCookie = http.get("/").headers[Header.SET_COOKIE]!!.first().substringBefore(";")
        connect("/", headers = mapOf(Header.COOKIE to sessionCookie))
        server.expect("sessionAttribute=session-value", "sessionAttributeMapSize=1")
    }

    @Test
    fun `queryParamMap does not throw`() = wsTest({
        it.routes.ws("/*") { ws ->
            ws.onConnect { ctx -> record("queryParamMap=${ctx.queryParamMap()}") }
        }
    }) {
        connect("/path/0")
        server.expect("queryParamMap={}")
    }

    @Test
    fun `cookies work`() = wsTest({
        it.routes.ws("/cookies") { ws ->
            ws.onConnect { ctx ->
                record("cookie=${ctx.cookie("name")}")
                record("cookieMapSize=${ctx.cookieMap().size}")
            }
        }
    }) {
        connect("/cookies", headers = mapOf(Header.COOKIE to "name=value; name2=value2; name3=value3"))
        server.expect("cookie=value", "cookieMapSize=3")
    }

    @Test
    fun `WsContext equals does not throw on non-WsContext comparison`() = wsTest({
        it.routes.ws("/ws") { ws ->
            ws.onConnect { ctx ->
                record("equalsString=${ctx.equals("a String")}")
                record("equalsNull=${ctx.equals(null)}")
                record("equalsSelf=${ctx == ctx}")
            }
        }
    }) {
        connect("/ws")
        server.expect("equalsString=false", "equalsNull=false", "equalsSelf=true")
    }
}
