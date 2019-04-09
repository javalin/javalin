/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import com.mashape.unirest.http.Unirest
import io.javalin.apibuilder.ApiBuilder.ws
import io.javalin.json.JavalinJson
import io.javalin.misc.SerializeableObject
import io.javalin.util.TestUtil
import io.javalin.websocket.WsContext
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.websocket.api.MessageTooLargeException
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import org.junit.Before
import org.junit.Test
import java.net.URI
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * This test could be better
 */
class TestWebSocket {

    private val contextPathJavalin = Javalin.create().configure { it.wsContextPath = "/websocket" }
    private val javalinWithWsLogger = Javalin.create().configure {
        it.wsLogger { ws ->
            ws.onConnect { ctx -> log.add(ctx.pathParam("param") + " connected") }
            ws.onClose { ctx -> log.add(ctx.pathParam("param") + " disconnected") }
        }
    }
    private var log = mutableListOf<String>()

    @Before
    fun resetLog() {
        log = ArrayList()
    }

    @Test
    fun `each connection receives a unique id`() = TestUtil.test(contextPathJavalin) { app, _ ->
        app.ws("/test-websocket-1") { ws ->
            ws.onConnect { ctx -> log.add(ctx.sessionId) }
            ws.onMessage { ctx -> log.add(ctx.sessionId) }
            ws.onClose { ctx -> log.add(ctx.sessionId) }
        }
        app.routes {
            ws("/test-websocket-2") { ws ->
                ws.onConnect { ctx -> log.add(ctx.sessionId) }
                ws.onMessage { ctx -> log.add(ctx.sessionId) }
                ws.onClose { ctx -> log.add(ctx.sessionId) }
            }
        }

        val testClient1_1 = TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/test-websocket-1"))
        val testClient1_2 = TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/test-websocket-1"))
        val testClient2_1 = TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/test-websocket-2"))

        doAndSleepWhile({ testClient1_1.connect() }, { !testClient1_1.isOpen })
        doAndSleepWhile({ testClient1_2.connect() }, { !testClient1_2.isOpen })
        doAndSleep { testClient1_1.send("A") }
        doAndSleep { testClient1_2.send("B") }
        doAndSleepWhile({ testClient1_1.close() }, { testClient1_1.isClosing })
        doAndSleepWhile({ testClient1_2.close() }, { testClient1_2.isClosing })
        doAndSleepWhile({ testClient2_1.connect() }, { !testClient2_1.isOpen })
        doAndSleepWhile({ testClient2_1.close() }, { testClient2_1.isClosing })

        // 3 clients and a lot of operations should only yield three unique identifiers for the clients
        val uniqueLog = HashSet(log)
        assertThat(uniqueLog).hasSize(3)
        for (id in uniqueLog) {
            assertThat(uniqueLog.count { it == id }).isEqualTo(1)
        }
    }

    @Test
    fun `general integration test`() = TestUtil.test(contextPathJavalin) { app, _ ->
        val userUsernameMap = LinkedHashMap<WsContext, Int>()
        val atomicInteger = AtomicInteger()
        app.ws("/test-websocket-1") { ws ->
            ws.onConnect { ctx ->
                userUsernameMap[ctx] = atomicInteger.getAndIncrement()
                log.add(userUsernameMap[ctx].toString() + " connected")
            }
            ws.onMessage { ctx ->
                val message = ctx.message()
                log.add(userUsernameMap[ctx].toString() + " sent '" + message + "' to server")
                userUsernameMap.forEach { client, _ -> doAndSleep { client.send("Server sent '" + message + "' to " + userUsernameMap[client]) } }
            }
            ws.onClose { ctx -> log.add(userUsernameMap[ctx].toString() + " disconnected") }
        }
        app.routes {
            ws("/test-websocket-2") { ws ->
                ws.onConnect { log.add("Connected to other endpoint") }
                ws.onClose { _ -> log.add("Disconnected from other endpoint") }
            }
        }

        val testClient1_1 = TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/test-websocket-1"))
        val testClient1_2 = TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/test-websocket-1"))
        val testClient2_1 = TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/test-websocket-2"))

        doAndSleepWhile({ testClient1_1.connect() }, { !testClient1_1.isOpen })
        doAndSleepWhile({ testClient1_2.connect() }, { !testClient1_2.isOpen })
        doAndSleep { testClient1_1.send("A") }
        doAndSleep { testClient1_2.send("B") }
        doAndSleepWhile({ testClient1_1.close() }, { testClient1_1.isClosing })
        doAndSleepWhile({ testClient1_2.close() }, { testClient1_2.isClosing })
        doAndSleepWhile({ testClient2_1.connect() }, { !testClient2_1.isOpen })
        doAndSleepWhile({ testClient2_1.close() }, { testClient2_1.isClosing })
        assertThat(log).containsExactlyInAnyOrder(
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
    fun `receive and send json messages`() = TestUtil.test(contextPathJavalin) { app, _ ->
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

        val testClient = TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/message"))
        doAndSleepWhile({ testClient.connect() }, { !testClient.isOpen })
        doAndSleep { testClient.send(clientMessageJson) }

        assertThat(receivedJson).isEqualTo(clientMessageJson)
        assertThat(receivedMessage).isNotNull
        assertThat(receivedMessage!!.value1).isEqualTo(clientMessage.value1)
        assertThat(receivedMessage!!.value2).isEqualTo(clientMessage.value2)
        assertThat(log.last()).isEqualTo(serverMessageJson)
    }

    @Test
    fun `binary messages`() = TestUtil.test(contextPathJavalin) { app, _ ->
        val byteDataToSend1 = (0 until 4096).shuffled().map { it.toByte() }.toByteArray()
        val byteDataToSend2 = (0 until 4096).shuffled().map { it.toByte() }.toByteArray()

        val receivedBinaryData = mutableListOf<ByteArray>()
        app.ws("/binary") { ws ->
            ws.onBinaryMessage { ctx ->
                receivedBinaryData.add(ctx.data.toByteArray())
            }
        }

        val testClient = TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/binary"))

        doAndSleepWhile({ testClient.connect() }, { !testClient.isOpen })
        doAndSleep { testClient.send(byteDataToSend1) }
        doAndSleep { testClient.send(byteDataToSend2) }
        doAndSleepWhile({ testClient.close() }, { testClient.isClosing })

        assertThat(receivedBinaryData).containsExactlyInAnyOrder(byteDataToSend1, byteDataToSend2)
    }

    @Test
    fun `routing and pathParams() work`() = TestUtil.test(contextPathJavalin) { app, _ ->
        app.ws("/params/:1") { ws -> ws.onConnect { ctx -> log.add(ctx.pathParam("1")) } }
        app.ws("/params/:1/test/:2/:3") { ws -> ws.onConnect { ctx -> log.add(ctx.pathParam("1") + " " + ctx.pathParam("2") + " " + ctx.pathParam("3")) } }
        app.ws("/*") { ws -> ws.onConnect { _ -> log.add("catchall") } } // this should not be triggered since all calls match more specific handlers
        connectAndDisconnect(TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/params/one")))
        connectAndDisconnect(TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/params/%E2%99%94")))
        connectAndDisconnect(TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/params/another/test/long/path")))
        assertThat(log).containsExactlyInAnyOrder("one", "♔", "another long path")
        assertThat(log).doesNotContain("catchall")
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
            ws.onConnect { ctx -> log.add("Header: " + ctx.header("Test")!!) }
            ws.onClose { ctx -> log.add("Closed connection from: " + ctx.host()!!) }
        }
        connectAndDisconnect(TestClient(URI.create("ws://localhost:" + app.port() + "/websocket"), mapOf("Test" to "HeaderParameter")))
        assertThat(log).containsExactlyInAnyOrder("Header: HeaderParameter", "Closed connection from: localhost")
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
        connectAndDisconnect(TestClient(URI.create("ws://localhost:" + app.port() + "/websocket/channel-one?qp=just-a-qp&qps=1&qps=2")))
        assertThat(matchedPath).isEqualTo("/websocket/:channel")
        assertThat(pathParam).isEqualTo("channel-one")
        assertThat(queryParam).isEqualTo("just-a-qp")
        assertThat(queryParams).contains("1", "2")
    }

    @Test
    fun `routing and path-params case sensitive works`() = TestUtil.test { app, _ ->
        app.ws("/pAtH/:param") { ws -> ws.onConnect { ctx -> log.add(ctx.pathParam("param")) } }
        app.ws("/other-path/:param") { ws -> ws.onConnect { ctx -> log.add(ctx.pathParam("param")) } }
        connectAndDisconnect(TestClient(URI.create("ws://localhost:" + app.port() + "/PaTh/my-param")))
        connectAndDisconnect(TestClient(URI.create("ws://localhost:" + app.port() + "/other-path/My-PaRaM")))
        assertThat(log).doesNotContain("my-param")
        assertThat(log).contains("My-PaRaM")
    }

    @Test
    fun `web socket logging works`() = TestUtil.test(javalinWithWsLogger) { app, _ ->
        app.ws("/path/:param") {}
        connectAndDisconnect(TestClient(URI.create("ws://localhost:" + app.port() + "/path/0")))
        connectAndDisconnect(TestClient(URI.create("ws://localhost:" + app.port() + "/path/1")))
        assertThat(log).containsExactlyInAnyOrder(
                "0 connected",
                "1 connected",
                "0 disconnected",
                "1 disconnected"
        )
    }

    @Test
    fun `debug logging works for web sockets`() = TestUtil.test(Javalin.create().enableDevLogging()) { app, _ ->
        app.ws("/path/:param") {}
        connectAndDisconnect(TestClient(URI.create("ws://localhost:" + app.port() + "/path/0")))
        connectAndDisconnect(TestClient(URI.create("ws://localhost:" + app.port() + "/path/1?test=banana&hi=1&hi=2")))
        assertThat(log.size).isEqualTo(0)
    }

    @Test
    fun `queryParamMap does not throw`() = TestUtil.test { app, _ ->
        app.ws("/*") { ws ->
            ws.onConnect { ctx ->
                try {
                    ctx.queryParamMap()
                } catch (e: Exception) {
                    log.add("exception queryParamMap")
                }
            }
        }

        connectAndDisconnect(TestClient(URI.create("ws://localhost:" + app.port() + "/path/0")))
        assertThat(log.size).isEqualTo(0)
    }

    @Test
    fun `custom WebSocketServletFactory works`() {
        val newServer = Server()
        var err: Throwable? = Exception("Bang")
        val maxTextSize = 1
        val textToSend = "This text is far too long."
        val expectedMessage = "Text message size [${textToSend.length}] exceeds maximum size [$maxTextSize]"
        val app = Javalin.create().configure {
            it.wsFactoryConfig { wsFactory ->
                wsFactory.policy.maxTextMessageSize = maxTextSize
            }
        }
        app.ws("/ws") { ws ->
            ws.onError { ctx -> err = ctx.error }
        }.configure {
            it.server { newServer }
        }.start(0)
        val testClient = TestClient(URI.create("ws://localhost:" + app.port() + "/ws"))

        doAndSleepWhile({ testClient.connect() }, { !testClient.isOpen })
        doAndSleep { testClient.send(textToSend) }
        doAndSleepWhile({ testClient.close() }, { testClient.isClosing })
        app.stop()

        assertThat(err!!.message).isEqualTo(expectedMessage)
        assertThat(err).isExactlyInstanceOf(MessageTooLargeException::class.java)
    }

    // ********************************************************************************************
    // Helpers
    // ********************************************************************************************

    internal inner class TestClient : WebSocketClient {
        constructor(serverUri: URI) : super(serverUri)
        constructor(serverUri: URI, headers: Map<String, String>) : super(serverUri, Draft_6455(), headers, 0)

        override fun onMessage(s: String) {
            log.add(s)
        }

        override fun onOpen(serverHandshake: ServerHandshake) {}
        override fun onClose(i: Int, s: String, b: Boolean) {}
        override fun onError(e: Exception) {}
    }

    private fun connectAndDisconnect(testClient: TestClient) {
        doAndSleepWhile({ testClient.connect() }, { !testClient.isOpen })
        doAndSleepWhile({ testClient.close() }, { testClient.isClosing })
    }

    private fun doAndSleepWhile(func1: () -> Unit, func2: () -> Boolean) {
        val timeMillis = System.currentTimeMillis()
        func1.invoke()
        while (func2.invoke() && System.currentTimeMillis() < timeMillis + 1000) {
            Thread.sleep(25)
        }
    }

    private fun doAndSleep(func: () -> Unit) {
        func.invoke()
        Thread.sleep(25)
    }

}
