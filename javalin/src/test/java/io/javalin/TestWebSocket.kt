/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.apibuilder.ApiBuilder.ws
import io.javalin.core.util.Header
import io.javalin.http.UnauthorizedResponse
import io.javalin.plugin.json.JavalinJson
import io.javalin.testing.SerializeableObject
import io.javalin.testing.TestUtil
import io.javalin.testing.TypedException
import io.javalin.websocket.WsContext
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.websocket.api.MessageTooLargeException
import org.eclipse.jetty.websocket.api.StatusCode
import org.eclipse.jetty.websocket.api.WebSocketConstants
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.junit.Test
import java.net.URI
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * This test could be better
 */
class TestWebSocket {

    data class TestLogger(val log: ArrayList<String>)

    private fun Javalin.logger(): TestLogger {
        if (this.attribute(TestLogger::class.java) == null) {
            this.attribute(TestLogger::class.java, TestLogger(ArrayList()))
        }
        return this.attribute(TestLogger::class.java)
    }

    fun contextPathJavalin() = Javalin.create { it.contextPath = "/websocket" }

    fun javalinWithWsLogger() = Javalin.create().apply {
        this.config.wsLogger { ws ->
            ws.onConnect { ctx -> this.logger().log.add(ctx.pathParam("param") + " connected") }
            ws.onClose { ctx -> this.logger().log.add(ctx.pathParam("param") + " disconnected") }
        }
    }

    fun accessManagedJavalin() = Javalin.create().apply {
        this.config.accessManager { handler, ctx, permittedRoles ->
            this.logger().log.add("handling upgrade request ...")
            when {
                ctx.queryParam("allowed") == "true" -> {
                    this.logger().log.add("upgrade request valid!")
                    handler.handle(ctx)
                }
                ctx.queryParam("exception") == "true" -> throw UnauthorizedResponse()
                else -> this.logger().log.add("upgrade request invalid!")
            }
        }
        this.ws("/*") { ws ->
            ws.onConnect { this.logger().log.add("connected with upgrade request") }
        }
    }

    @Test
    fun `each connection receives a unique id`() = TestUtil.test(contextPathJavalin()) { app, _ ->
        app.ws("/test-websocket-1") { ws ->
            ws.onConnect { ctx -> app.logger().log.add(ctx.sessionId) }
            ws.onMessage { ctx -> app.logger().log.add(ctx.sessionId) }
            ws.onClose { ctx -> app.logger().log.add(ctx.sessionId) }
        }
        app.routes {
            ws("/test-websocket-2") { ws ->
                ws.onConnect { ctx -> app.logger().log.add(ctx.sessionId) }
                ws.onMessage { ctx -> app.logger().log.add(ctx.sessionId) }
                ws.onClose { ctx -> app.logger().log.add(ctx.sessionId) }
            }
        }

        val testClient1_1 = TestClient(app, "/websocket/test-websocket-1")
        val testClient1_2 = TestClient(app, "/websocket/test-websocket-1")
        val testClient2_1 = TestClient(app, "/websocket/test-websocket-2")

        doAndSleepWhile({ testClient1_1.connect() }, { !testClient1_1.isOpen })
        doAndSleepWhile({ testClient1_2.connect() }, { !testClient1_2.isOpen })
        doAndSleep { testClient1_1.send("A") }
        doAndSleep { testClient1_2.send("B") }
        doAndSleepWhile({ testClient1_1.close() }, { testClient1_1.isClosing })
        doAndSleepWhile({ testClient1_2.close() }, { testClient1_2.isClosing })
        doAndSleepWhile({ testClient2_1.connect() }, { !testClient2_1.isOpen })
        doAndSleepWhile({ testClient2_1.close() }, { testClient2_1.isClosing })

        // 3 clients and a lot of operations should only yield three unique identifiers for the clients
        val uniqueLog = HashSet(app.logger().log)
        assertThat(uniqueLog).hasSize(3)
        uniqueLog.forEach { id -> assertThat(uniqueLog.count { it == id }).isEqualTo(1) }
    }

    @Test
    fun `general integration test`() = TestUtil.test(contextPathJavalin()) { app, _ ->
        val userUsernameMap = LinkedHashMap<WsContext, Int>()
        val atomicInteger = AtomicInteger()
        app.ws("/test-websocket-1") { ws ->
            ws.onConnect { ctx ->
                userUsernameMap[ctx] = atomicInteger.getAndIncrement()
                app.logger().log.add(userUsernameMap[ctx].toString() + " connected")
            }
            ws.onMessage { ctx ->
                val message = ctx.message()
                app.logger().log.add(userUsernameMap[ctx].toString() + " sent '" + message + "' to server")
                userUsernameMap.forEach { client, _ -> doAndSleep { client.send("Server sent '" + message + "' to " + userUsernameMap[client]) } }
            }
            ws.onClose { ctx -> app.logger().log.add(userUsernameMap[ctx].toString() + " disconnected") }
        }
        app.routes {
            ws("/test-websocket-2") { ws ->
                ws.onConnect { app.logger().log.add("Connected to other endpoint") }
                ws.onClose { _ -> app.logger().log.add("Disconnected from other endpoint") }
            }
        }

        val testClient1_1 = TestClient(app, "/websocket/test-websocket-1")
        val testClient1_2 = TestClient(app, "/websocket/test-websocket-1")
        val testClient2_1 = TestClient(app, "/websocket/test-websocket-2")

        doAndSleepWhile({ testClient1_1.connect() }, { !testClient1_1.isOpen })
        doAndSleepWhile({ testClient1_2.connect() }, { !testClient1_2.isOpen })
        doAndSleep { testClient1_1.send("A") }
        doAndSleep { testClient1_2.send("B") }
        doAndSleepWhile({ testClient1_1.close() }, { testClient1_1.isClosing })
        doAndSleepWhile({ testClient1_2.close() }, { testClient1_2.isClosing })
        doAndSleepWhile({ testClient2_1.connect() }, { !testClient2_1.isOpen })
        doAndSleepWhile({ testClient2_1.close() }, { testClient2_1.isClosing })
        Thread.sleep(50)
        assertThat(app.logger().log).containsExactlyInAnyOrder(
                "0 connected",
                "1 connected",
                "0 sent 'A' to server",
                "Server sent 'A' to 0",
                "Server sent 'A' to 1",
                "1 sent 'B' to server",
                "Server sent 'B' to 0",
                "Server sent 'B' to 1",
                "0 disconnected",
                "1 disconnected",
                "Connected to other endpoint",
                "Disconnected from other endpoint"
        )
    }

    @Test
    fun `receive and send json messages`() = TestUtil.test(contextPathJavalin()) { app, _ ->
        val clientMessage = SerializeableObject().apply { value1 = "test1"; value2 = "test2" }
        val clientMessageJson = JavalinJson.toJson(clientMessage)

        val serverMessage = SerializeableObject().apply { value1 = "echo1"; value2 = "echo2" }
        val serverMessageJson = JavalinJson.toJson(serverMessage)

        var receivedJson: String? = null
        var receivedMessage: SerializeableObject? = null
        app.ws("/message") { ws ->
            ws.onMessage { ctx ->
                receivedJson = ctx.message()
                receivedMessage = ctx.message<SerializeableObject>()
                ctx.send(serverMessage)
            }
        }

        val testClient = TestClient(app, "/websocket/message")
        doAndSleepWhile({ testClient.connect() }, { !testClient.isOpen })
        doAndSleep { testClient.send(clientMessageJson) }

        assertThat(receivedJson).isEqualTo(clientMessageJson)
        assertThat(receivedMessage).isNotNull
        assertThat(receivedMessage!!.value1).isEqualTo(clientMessage.value1)
        assertThat(receivedMessage!!.value2).isEqualTo(clientMessage.value2)
        assertThat(app.logger().log.last()).isEqualTo(serverMessageJson)
    }

    @Test
    fun `binary messages`() = TestUtil.test(contextPathJavalin()) { app, _ ->
        val byteDataToSend1 = (0 until 4096).shuffled().map { it.toByte() }.toByteArray()
        val byteDataToSend2 = (0 until 4096).shuffled().map { it.toByte() }.toByteArray()

        val receivedBinaryData = mutableListOf<ByteArray>()
        app.ws("/binary") { ws ->
            ws.onBinaryMessage { ctx ->
                receivedBinaryData.add(ctx.data())
            }
        }

        val testClient = TestClient(app, "/websocket/binary")

        doAndSleepWhile({ testClient.connect() }, { !testClient.isOpen })
        doAndSleep { testClient.send(byteDataToSend1) }
        doAndSleep { testClient.send(byteDataToSend2) }
        doAndSleepWhile({ testClient.close() }, { testClient.isClosing })

        assertThat(receivedBinaryData).containsExactlyInAnyOrder(byteDataToSend1, byteDataToSend2)
    }

    @Test
    fun `routing and pathParams work`() = TestUtil.test(contextPathJavalin()) { app, _ ->
        app.ws("/params/:1") { ws -> ws.onConnect { ctx -> app.logger().log.add(ctx.pathParam("1")) } }
        app.ws("/params/:1/test/:2/:3") { ws -> ws.onConnect { ctx -> app.logger().log.add(ctx.pathParam("1") + " " + ctx.pathParam("2") + " " + ctx.pathParam("3")) } }
        app.ws("/*") { ws -> ws.onConnect { _ -> app.logger().log.add("catchall") } } // this should not be triggered since all calls match more specific handlers
        TestClient(app, "/websocket/params/one").connectAndDisconnect()
        TestClient(app, "/websocket/params/%E2%99%94").connectAndDisconnect()
        TestClient(app, "/websocket/params/another/test/long/path").connectAndDisconnect()
        assertThat(app.logger().log).containsExactlyInAnyOrder("one", "♔", "another long path")
        assertThat(app.logger().log).doesNotContain("catchall")
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
    fun `headers and host are available in session`() = TestUtil.test { app, _ ->
        app.ws("/websocket") { ws ->
            ws.onConnect { ctx -> app.logger().log.add("Header: " + ctx.header("Test")!!) }
            ws.onClose { ctx -> app.logger().log.add("Closed connection from: " + ctx.host()!!) }
        }
        TestClient(app, "/websocket", mapOf("Test" to "HeaderParameter")).connectAndDisconnect()
        assertThat(app.logger().log).containsExactlyInAnyOrder("Header: HeaderParameter", "Closed connection from: localhost")
    }

    @Test
    fun `extracting path information works`() = TestUtil.test { app, _ ->
        var matchedPath = ""
        var pathParam = ""
        var queryParam = ""
        var queryParams = listOf<String>()
        app.ws("/websocket/:channel") { ws ->
            ws.onConnect { ctx ->
                matchedPath = ctx.matchedPath()
                pathParam = ctx.pathParam("channel")
                queryParam = ctx.queryParam("qp")!!
                queryParams = ctx.queryParams("qps")
            }
        }
        TestClient(app, "/websocket/channel-one?qp=just-a-qp&qps=1&qps=2").connectAndDisconnect()
        assertThat(matchedPath).isEqualTo("/websocket/:channel")
        assertThat(pathParam).isEqualTo("channel-one")
        assertThat(queryParam).isEqualTo("just-a-qp")
        assertThat(queryParams).contains("1", "2")
    }

    @Test
    fun `extracting path information works in all handlers`() = TestUtil.test { app, _ ->
        app.ws("/context-life") { ws ->
            ws.onConnect { app.logger().log.add(it.queryParam("qp")!! + 1) }
            ws.onMessage { app.logger().log.add(it.queryParam("qp")!! + 2) }
            ws.onClose { app.logger().log.add(it.queryParam("qp")!! + 3) }
        }
        val client = TestClient(app, "/context-life?qp=great")
        doAndSleepWhile({ client.connect() }, { !client.isOpen })
        doAndSleep { client.send("not-important") }
        doAndSleepWhile({ client.close() }, { client.isClosing })
        assertThat(app.logger().log).containsExactly("great1", "great2", "great3")
    }

    @Test
    fun `set attributes works`() = TestUtil.test { app, _ ->
        app.ws("/attributes") { ws ->
            ws.onConnect { it.attribute("test", "Success") }
            ws.onClose { app.logger().log.add(it.attribute("test") ?: "Fail") }
        }
        TestClient(app, "/attributes").connectAndDisconnect()
        assertThat(app.logger().log).containsExactly("Success")
    }

    @Test
    fun `getting session attributes works`() = TestUtil.test { app, http ->
        app.get("/") { ctx -> ctx.sessionAttribute("session-key", "session-value") }
        app.ws("/") { ws ->
            ws.onConnect {
                app.logger().log.add(it.sessionAttribute("session-key")!!)
                app.logger().log.add("sessionAttributeMapSize:${it.sessionAttributeMap().size}")
            }
        }
        val sessionCookie = http.get("/").headers["Set-Cookie"]!!.first().removePrefix("JSESSIONID=")
        TestClient(app, "/", mapOf("Cookie" to "JSESSIONID=${sessionCookie}")).connectAndDisconnect()
        assertThat(app.logger().log).containsExactly("session-value", "sessionAttributeMapSize:1")
    }

    @Test
    fun `routing and path-params case sensitive works`() = TestUtil.test { app, _ ->
        app.ws("/pAtH/:param") { ws -> ws.onConnect { ctx -> app.logger().log.add(ctx.pathParam("param")) } }
        app.ws("/other-path/:param") { ws -> ws.onConnect { ctx -> app.logger().log.add(ctx.pathParam("param")) } }

        val client = TestClient(app, "/PaTh/my-param")

        doAndSleepWhile({ client.connect() }, { !client.isClosed })

        TestClient(app, "/other-path/My-PaRaM").connectAndDisconnect()

        assertThat(app.logger().log).doesNotContain("my-param")
        assertThat(app.logger().log).contains("My-PaRaM")
    }

    @Test
    fun `web socket logging works`() = TestUtil.test(javalinWithWsLogger()) { app, _ ->
        app.ws("/path/:param") {}
        TestClient(app, "/path/0").connectAndDisconnect()
        TestClient(app, "/path/1").connectAndDisconnect()
        assertThat(app.logger().log).containsExactlyInAnyOrder(
                "0 connected",
                "1 connected",
                "0 disconnected",
                "1 disconnected"
        )
    }

    @Test
    fun `dev logging works for web sockets`() = TestUtil.test(Javalin.create { it.enableDevLogging() }) { app, _ ->
        app.ws("/path/:param") {}
        TestClient(app, "/path/0").connectAndDisconnect()
        TestClient(app, "/path/1?test=banana&hi=1&hi=2").connectAndDisconnect()
        assertThat(app.logger().log.size).isEqualTo(0)
    }

    @Test
    fun `queryParamMap does not throw`() = TestUtil.test { app, _ ->
        app.ws("/*") { ws ->
            ws.onConnect { ctx ->
                ctx.queryParamMap()
                app.logger().log.add("call succeeded")
            }
        }

        TestClient(app, "/path/0").connectAndDisconnect()
        assertThat(app.logger().log).contains("call succeeded")
    }

    @Test
    fun `custom WebSocketServletFactory works`() {
        var err: Throwable? = Exception("Bang")
        val maxTextSize = 1
        val textToSend = "This text is far too long."
        val expectedMessage = "Text message size [${textToSend.length}] exceeds maximum size [$maxTextSize]"
        val app = Javalin.create {
            it.wsFactoryConfig { wsFactory ->
                wsFactory.policy.maxTextMessageSize = maxTextSize
            }
        }.ws("/ws") { ws ->
            ws.onError { ctx -> err = ctx.error() }
        }.start(0)

        val testClient = TestClient(app, "/ws")
        doAndSleepWhile({ testClient.connect() }, { !testClient.isOpen })
        doAndSleep { testClient.send(textToSend) }
        doAndSleepWhile({ testClient.close() }, { testClient.isClosing })
        app.stop()

        assertThat(err!!.message).isEqualTo(expectedMessage)
        assertThat(err).isExactlyInstanceOf(MessageTooLargeException::class.java)
    }

    @Test
    fun `AccessManager rejects invalid request`() = TestUtil.test(accessManagedJavalin()) { app, _ ->
        val client = TestClient(app, "/")

        doAndSleepWhile({ client.connect() }, { !client.isClosed })

        assertThat(app.logger().log.size).isEqualTo(2)
        assertThat(app.logger().log).containsExactlyInAnyOrder("handling upgrade request ...", "upgrade request invalid!")
    }

    @Test
    fun `AccessManager accepts valid request`() = TestUtil.test(accessManagedJavalin()) { app, _ ->
        TestClient(app, "/?allowed=true").connectAndDisconnect()
        assertThat(app.logger().log.size).isEqualTo(3)
        assertThat(app.logger().log).containsExactlyInAnyOrder("handling upgrade request ...", "upgrade request valid!", "connected with upgrade request")
    }

    @Test
    fun `AccessManager doesn't crash on exception`() = TestUtil.test(accessManagedJavalin()) { app, _ ->
        val client = TestClient(app, "/?exception=true")

        doAndSleepWhile({ client.connect() }, { !client.isClosed })

        assertThat(app.logger().log.size).isEqualTo(1)
    }

    @Test
    fun `cookies work`() = TestUtil.test { app, _ ->
        app.ws("/cookies") { ws ->
            ws.onConnect { ctx ->
                app.logger().log.add(ctx.cookie("name")!!)
                app.logger().log.add("cookieMapSize:${ctx.cookieMap().size}")
            }
        }
        TestClient(app, "/cookies", mapOf("Cookie" to "name=value; name2=value2; name3=value3")).connectAndDisconnect()
        assertThat(app.logger().log).containsExactly("value", "cookieMapSize:3")
    }

    @Test
    fun `before handlers work`() = TestUtil.test { app, _ ->
        app.wsBefore { ws ->
            ws.onConnect { app.logger().log.add("before handler: onConnect") }
            ws.onMessage { app.logger().log.add("before handler: onMessage") }
            ws.onClose { app.logger().log.add("before handler: onClose") }
        }

        app.ws("/ws") { ws ->
            ws.onConnect { app.logger().log.add("endpoint handler: onConnect") }
            ws.onMessage { app.logger().log.add("endpoint handler: onMessage") }
            ws.onClose { app.logger().log.add("endpoint handler: onClose") }
        }

        val client = TestClient(app, "/ws")

        doAndSleepWhile({ client.connect() }, { !client.isOpen })
        doAndSleep { client.send("test") }
        doAndSleepWhile({ client.close() }, { client.isClosing })

        assertThat(app.logger().log).containsExactly(
                "before handler: onConnect", "endpoint handler: onConnect",
                "before handler: onMessage", "endpoint handler: onMessage",
                "before handler: onClose", "endpoint handler: onClose"
        )
    }

    @Test
    fun `throw in wsBefore short circuits endpoint handler`() = TestUtil.test { app, _ ->
        app.wsBefore { it.onConnect { throw UnauthorizedResponse() } }
        app.ws("/ws") { it.onConnect { app.logger().log.add("This should not be added") } }
        app.wsException(Exception::class.java) { e, _ -> app.logger().log.add(e.message!!) }
        TestClient(app, "/ws").connectAndDisconnect()
        assertThat(app.logger().log).contains("Unauthorized")
        assertThat(app.logger().log).doesNotContain("This should not be added")
    }

    @Test
    fun `wsBefore with path works`() = TestUtil.test { app, _ ->
        app.wsBefore("/ws/*") { it.onConnect { app.logger().log.add("Before!") } }
        app.ws("/ws/test") { it.onConnect { app.logger().log.add("Endpoint!") } }
        TestClient(app, "/ws/test").connectAndDisconnect()
        assertThat(app.logger().log).containsExactly("Before!", "Endpoint!")
    }

    @Test
    fun `multiple before and after handlers can be called`() = TestUtil.test { app, _ ->
        app.wsBefore { it.onConnect { app.logger().log.add("Before 1") } }
        app.wsBefore("/ws/*") { it.onConnect { app.logger().log.add("Before 2") } }
        app.wsAfter { it.onConnect { app.logger().log.add("After 1") } }
        app.wsAfter("/ws/*") { it.onConnect { app.logger().log.add("After 2") } }
        app.ws("/ws/test") { it.onConnect { app.logger().log.add("Endpoint") } }
        TestClient(app, "/ws/test").connectAndDisconnect()
        assertThat(app.logger().log).containsExactly("Before 1", "Before 2", "Endpoint", "After 1", "After 2")
    }

    @Test
    fun `after handlers work`() = TestUtil.test { app, _ ->
        app.ws("/ws") { ws ->
            ws.onConnect { app.logger().log.add("endpoint handler: onConnect") }
            ws.onMessage { app.logger().log.add("endpoint handler: onMessage") }
            ws.onClose { app.logger().log.add("endpoint handler: onClose") }
        }

        app.wsAfter { ws ->
            ws.onConnect { app.logger().log.add("after handler: onConnect") }
            ws.onMessage { app.logger().log.add("after handler: onMessage") }
            ws.onClose { app.logger().log.add("after handler: onClose") }
        }

        val client = TestClient(app, "/ws")

        doAndSleepWhile({ client.connect() }, { !client.isOpen })
        doAndSleep { client.send("test") }
        doAndSleepWhile({ client.close() }, { client.isClosing })

        assertThat(app.logger().log).containsExactly(
                "endpoint handler: onConnect", "after handler: onConnect",
                "endpoint handler: onMessage", "after handler: onMessage",
                "endpoint handler: onClose", "after handler: onClose"
        )
    }

    @Test
    fun `unmapped exceptions are caught by default handler`() = TestUtil.test { app, _ ->
        val exception = Exception("Error message")

        app.ws("/ws") { it.onConnect { throw exception } }

        val client = object : TestClient(app, "/ws") {
            override fun onClose(i: Int, s: String, b: Boolean) {
                this.app.logger().log.add("Status code: $i")
                this.app.logger().log.add("Reason: $s")
            }
        }

        doAndSleepWhile({ client.connect() }, { !client.isClosed })

        assertThat(client.app.logger().log).containsExactly(
                "Status code: ${StatusCode.SERVER_ERROR}",
                "Reason: ${exception.message}"
        )
    }

    @Test
    fun `mapped exceptions are handled`() = TestUtil.test { app, _ ->
        app.ws("/ws") { it.onConnect { throw Exception() } }
        app.wsException(Exception::class.java) { _, _ -> app.logger().log.add("Exception handler called") }
        TestClient(app, "/ws").connectAndDisconnect()
        assertThat(app.logger().log).containsExactly("Exception handler called")
    }

    @Test
    fun `most specific exception handler handles exception`() = TestUtil.test { app, _ ->
        app.ws("/ws") { it.onConnect { throw TypedException() } }
        app.wsException(Exception::class.java) { _, _ -> app.logger().log.add("Exception handler called") }
        app.wsException(TypedException::class.java) { _, _ -> app.logger().log.add("TypedException handler called") }
        TestClient(app, "/ws").connectAndDisconnect()
        assertThat(app.logger().log).containsExactly("TypedException handler called")
    }

    @Test
    fun `websocket subprotocol is set if included`() = TestUtil.test { app, http ->
        app.ws("/ws") {}
        val response = Unirest.get("http://localhost:${app.port()}/ws")
                .header(Header.SEC_WEBSOCKET_KEY, "not-null")
                .header(WebSocketConstants.SEC_WEBSOCKET_PROTOCOL, "mqtt")
                .asString()
        assertThat(response.headers.getFirst(WebSocketConstants.SEC_WEBSOCKET_PROTOCOL)).isEqualTo("mqtt")
    }

    // ********************************************************************************************
    // Helpers
    // ********************************************************************************************

    internal open inner class TestClient(var app: Javalin, path: String, headers: Map<String, String> = emptyMap()) : WebSocketClient(URI.create("ws://localhost:" + app.port() + path), Draft_6455(), headers, 0) {

        override fun onOpen(serverHandshake: ServerHandshake) {}
        override fun onClose(i: Int, s: String, b: Boolean) {}
        override fun onError(e: Exception) {}
        override fun onMessage(s: String) {
            app.logger().log.add(s)
        }

        fun connectAndDisconnect() {
            doAndSleepWhile({ connect() }, { !isOpen })
            doAndSleepWhile({ close() }, { !isClosed })
        }
    }

    private fun doAndSleepWhile(slowFunction: () -> Unit, conditionFunction: () -> Boolean, timeout: Duration = Duration.ofSeconds(5)) {
        val startTime = System.currentTimeMillis()
        val limitTime = startTime + timeout.toMillis();

        slowFunction.invoke()

        while (conditionFunction.invoke()) {
            if (System.currentTimeMillis() > limitTime) {
                throw TimeoutException("Wait for condition has timed out")
            }

            Thread.sleep(2)
        }
    }

    private fun doAndSleep(func: () -> Unit) = func.invoke().also { Thread.sleep(50) }

}
