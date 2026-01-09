/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

@Timeout(value = 2, unit = TimeUnit.SECONDS)
class TestWsContext {

    @Test
    fun `headers and host are available in context`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/websocket") { ws ->
            ws.onConnect { ctx -> log.add("Header: " + ctx.header("Test")!!) }
            ws.onClose { ctx -> log.add("Closed connection from: " + ctx.host()) }
        }
        WsTestClient(app, "/websocket", mapOf("Test" to "HeaderParameter")).connectAndDisconnect()
        assertThat(log).containsExactlyInAnyOrder("Header: HeaderParameter", "Closed connection from: localhost:${app.port()}")
    }

    @Test
    fun `extracting path information works`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/websocket/{channel}") { ws ->
            ws.onConnect { ctx ->
                log.add("matchedPath=${ctx.endpoint().path}")
                log.add("pathParam=${ctx.pathParam("channel")}")
                log.add("queryParam=${ctx.queryParam("qp")}")
                log.add("queryParams=${ctx.queryParams("qps")}")
            }
        }
        WsTestClient(app, "/websocket/channel-one?qp=just-a-qp&qps=1&qps=2").connectAndDisconnect()
        assertThat(log).containsExactly(
            "matchedPath=/websocket/{channel}",
            "pathParam=channel-one",
            "queryParam=just-a-qp",
            "queryParams=[1, 2]"
        )
    }

    @Test
    fun `extracting path information works in all handlers`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/context-life") { ws ->
            ws.onConnect { log.add("${it.queryParam("qp")}-connect") }
            ws.onMessage { log.add("${it.queryParam("qp")}-message") }
            ws.onClose { log.add("${it.queryParam("qp")}-close") }
        }
        WsTestClient(app, "/context-life?qp=great").connectSendAndDisconnect("not-important")
        assertThat(log).containsExactly("great-connect", "great-message", "great-close")
    }

    @Test
    fun `set attributes works`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/attributes") { ws ->
            ws.onConnect { it.attribute("test", "Success") }
            ws.onClose { log.add(it.attribute("test") ?: "Fail") }
        }
        WsTestClient(app, "/attributes").connectAndDisconnect()
        assertThat(log).containsExactly("Success")
    }

    @Test
    fun `getting session attributes works`() = TestUtil.test { app, http ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.get("/") { it.sessionAttribute("session-key", "session-value") }
        app.unsafe.routes.ws("/") { ws ->
            ws.onConnect {
                log.add(it.sessionAttribute("session-key")!!)
                log.add("sessionAttributeMapSize:${it.sessionAttributeMap().size}")
            }
        }
        val sessionCookie = http.get("/").headers["Set-Cookie"]!!.first().removePrefix("JSESSIONID=")
        WsTestClient(app, "/", mapOf("Cookie" to "JSESSIONID=${sessionCookie}")).connectAndDisconnect()
        assertThat(log).containsExactly("session-value", "sessionAttributeMapSize:1")
    }

    @Test
    fun `queryParamMap does not throw`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/*") { ws ->
            ws.onConnect { ctx ->
                ctx.queryParamMap()
                log.add("call succeeded")
            }
        }
        WsTestClient(app, "/path/0").connectAndDisconnect()
        assertThat(log).containsExactly("call succeeded")
    }

    @Test
    fun `cookies work`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/cookies") { ws ->
            ws.onConnect { ctx ->
                log.add(ctx.cookie("name")!!)
                log.add("cookieMapSize:${ctx.cookieMap().size}")
            }
        }
        WsTestClient(app, "/cookies", mapOf("Cookie" to "name=value; name2=value2; name3=value3")).connectAndDisconnect()
        assertThat(log).containsExactly("value", "cookieMapSize:3")
    }

    @Test
    fun `WsContext equals does not throw on non-WsContext comparison`() = TestUtil.test { app, _ ->
        val log = ConcurrentLinkedQueue<String>()
        app.unsafe.routes.ws("/ws") { ws ->
            ws.onConnect { ctx ->
                log.add("equalsString=${ctx.equals("a String")}")
                log.add("equalsNull=${ctx.equals(null)}")
                log.add("equalsSelf=${ctx == ctx}")
            }
        }
        WsTestClient(app, "/ws").connectAndDisconnect()
        assertThat(log).containsExactly("equalsString=false", "equalsNull=false", "equalsSelf=true")
    }
}

