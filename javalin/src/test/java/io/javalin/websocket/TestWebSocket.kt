package io.javalin.websocket

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder
import io.javalin.http.Header
import io.javalin.http.UnauthorizedResponse
import io.javalin.json.toJsonString
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import io.javalin.testing.fasterJacksonMapper
import kong.unirest.Unirest
import org.assertj.core.api.Assertions
import org.eclipse.jetty.util.BufferUtil
import org.eclipse.jetty.websocket.api.exceptions.MessageTooLargeException
import org.eclipse.jetty.websocket.api.util.WebSocketConstants
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@Timeout(value = 2, unit = TimeUnit.SECONDS)
class TestWebSocket {

    @Test
    fun `each connection receives a unique id`() {
        val log = ConcurrentLinkedQueue<String>()
        TestUtil.test(contextPathJavalin { cfg ->
            cfg.routes.ws("/test-websocket-1") { ws ->
                ws.onConnect { ctx -> log.add(ctx.sessionId()) }
                ws.onClose { ctx -> log.add(ctx.sessionId()) }
            }
            cfg.routes.apiBuilder {
                ApiBuilder.ws("/test-websocket-2") { ws ->
                    ws.onConnect { ctx -> log.add(ctx.sessionId()) }
                    ws.onClose { ctx -> log.add(ctx.sessionId()) }
                }
            }
        }) { app, _ ->
            WsTestClient(app, "/websocket/test-websocket-1").connectAndDisconnect()
            WsTestClient(app, "/websocket/test-websocket-1").connectAndDisconnect()
            WsTestClient(app, "/websocket/test-websocket-2").connectAndDisconnect()
            // 3 clients and 6 operations should yield 3 unique identifiers total
            val uniqueLog = HashSet<String>(log)
            Assertions.assertThat(uniqueLog).hasSize(3)
            uniqueLog.forEach { id: String -> Assertions.assertThat(uniqueLog.count { it == id }).isEqualTo(1) }
        }
    }

    @Test
    fun `general integration test`() {
        val log = ConcurrentLinkedQueue<String>()
        val idMap = ConcurrentHashMap<WsContext, Int>()
        val atomicInteger = AtomicInteger()

        TestUtil.test(contextPathJavalin { cfg ->
            cfg.routes.ws("/test-websocket-1") { ws ->
                    ws.onConnect { ctx ->
                        idMap[ctx] = atomicInteger.getAndIncrement()
                        log.add("${idMap[ctx]} connected")
                    }
                    ws.onMessage { ctx ->
                        log.add("${idMap[ctx]} sent '${ctx.message()}' to server")
                        idMap.forEach { (client, _) -> client.send("Server sent '${ctx.message()}' to ${idMap[client]}") }
                    }
                    ws.onClose { ctx -> log.add("${idMap[ctx]} disconnected") }
                }
            cfg.routes.apiBuilder { // use .routes to test apibuilder
                ApiBuilder.ws("/test-websocket-2") { ws ->
                    ws.onConnect { log.add("Connected to other endpoint") }
                    ws.onClose { log.add("Disconnected from other endpoint") }
                }
            }
        }) { app, _ ->
            val testClient0 = WsTestClient(app, "/websocket/test-websocket-1", onMessage = { log.add(it) }).also { it.connectBlocking() }
            val testClient1 = WsTestClient(app, "/websocket/test-websocket-1", onMessage = { log.add(it) }).also { it.connectBlocking() }
            awaitCondition(condition = { log.size >= 8 }) { // wait for 8 entries: 2 connects + 2 server messages + 4 client echoes
                testClient0.send("A")
                testClient1.send("B")
            }
            testClient0.closeBlocking()
            testClient1.closeBlocking()
            WsTestClient(app, "/websocket/test-websocket-2").connectAndDisconnect()
            // Message order is non-deterministic due to concurrency
            Assertions.assertThat(log).contains("0 connected", "1 connected")
            Assertions.assertThat(log).containsAnyOf("0 sent 'A' to server", "0 sent 'B' to server")
            Assertions.assertThat(log).containsAnyOf("1 sent 'A' to server", "1 sent 'B' to server")
            Assertions.assertThat(log).contains(
                "Server sent 'A' to 0", "Server sent 'A' to 1",
                "Server sent 'B' to 0", "Server sent 'B' to 1"
            )
            Assertions.assertThat(log).contains("0 disconnected", "1 disconnected")
            Assertions.assertThat(log).contains("Connected to other endpoint", "Disconnected from other endpoint")
            Assertions.assertThat(log).hasSize(12)
        }
    }

    @Test
    fun `receive and send json messages`() = TestUtil.test(Javalin.create {
        it.jsonMapper(fasterJacksonMapper)
        it.routes.ws("/message") { ws ->
            ws.onMessage { ctx ->
                val receivedMessage = ctx.messageAsClass<SerializableObject>()
                receivedMessage.value1 = "updated"
                ctx.send(receivedMessage)
            }
        }
    }) { app, _ ->
        val clientJsonString = fasterJacksonMapper.toJsonString(SerializableObject().apply { value1 = "test1"; value2 = "test2" })
        var response: String? = null
        val testClient = WsTestClient(app, "/message").also {
            it.onMessage = { msg -> response = msg }
            it.connectBlocking()
        }
        awaitCondition(condition = { response != null }) { testClient.send(clientJsonString) }
        Assertions.assertThat(response).contains(""""value1":"updated"""")
        Assertions.assertThat(response).contains(""""value2":"test2"""")
    }

    @Test
    fun `binary messages`() = TestUtil.test(contextPathJavalin()) { app, _ ->
        val byteDataToSend1 = (0 until 4096).shuffled().map { it.toByte() }.toByteArray()
        val byteDataToSend2 = (0 until 4096).shuffled().map { it.toByte() }.toByteArray()
        val receivedBinaryData = mutableListOf<ByteArray>()
        app.unsafe.routes.ws("/binary") { ws ->
            ws.onBinaryMessage { ctx ->
                val data = BufferUtil.toArray(ctx.data())
                receivedBinaryData.add(data)
            }
        }
        WsTestClient(app, "/websocket/binary").also {
            it.connectBlocking()
            it.send(byteDataToSend1)
            it.send(byteDataToSend2)
            it.closeBlocking()
        }
        Assertions.assertThat(receivedBinaryData).containsExactlyInAnyOrder(byteDataToSend1, byteDataToSend2)
    }

    @Test
    fun `custom WebSocketServletFactory works`() {
        var handlerError: Throwable? = null
        val maxTextSize = 1L
        val textToSend = "This text is far too long."
        val app = Javalin.create {
            it.jetty.modifyWebSocketServletFactory { wsFactory ->
                wsFactory.maxTextMessageSize = maxTextSize
            }
            it.routes.ws("/ws") { ws ->
                ws.onError { ctx -> handlerError = ctx.error() }
            }
        }
        TestUtil.test(app) { _, _ ->
            WsTestClient(app, "/ws").connectSendAndDisconnect(textToSend)
            repeat(10) {
                if (handlerError == null) Thread.sleep(5) // give Javalin time to trigger the error handler
            }
            Assertions.assertThat(handlerError!!.message).isEqualTo("Text message too large: ${textToSend.length} > $maxTextSize")
            Assertions.assertThat(handlerError).isExactlyInstanceOf(MessageTooLargeException::class.java)
        }
    }

    private fun accessManagedJavalin(log: ConcurrentLinkedQueue<String>): Javalin = Javalin.create {
        it.routes.wsBeforeUpgrade { ctx ->
            log.add("handling upgrade request ...")
            when {
                ctx.queryParam("exception") == "true" -> throw UnauthorizedResponse()
                ctx.queryParam("allowed") == "true" -> {
                    log.add("upgrade request valid!")
                    return@wsBeforeUpgrade
                }
                else -> {
                    log.add("upgrade request invalid!")
                    ctx.skipRemainingHandlers()
                }
            }
        }
        it.routes.ws("/*") { ws ->
            ws.onConnect { log.add("connected with upgrade request") }
        }
    }

    @Test
    fun `AccessManager rejects invalid request`() {
        val log = ConcurrentLinkedQueue<String>()
        TestUtil.test(accessManagedJavalin(log)) { app, _ ->
            WsTestClient(app, "/").connectAndDisconnect()
            Assertions.assertThat(log).hasSize(2)
            Assertions.assertThat(log).containsExactlyInAnyOrder("handling upgrade request ...", "upgrade request invalid!")
        }
    }

    @Test
    fun `AccessManager accepts valid request`() {
        val log = ConcurrentLinkedQueue<String>()
        TestUtil.test(accessManagedJavalin(log)) { app, _ ->
            WsTestClient(app, "/?allowed=true").connectAndDisconnect()
            Assertions.assertThat(log).hasSize(3)
            Assertions.assertThat(log).containsExactlyInAnyOrder("handling upgrade request ...", "upgrade request valid!", "connected with upgrade request")
        }
    }

    @Test
    fun `AccessManager doesn't crash on exception`() {
        val log = ConcurrentLinkedQueue<String>()
        TestUtil.test(accessManagedJavalin(log)) { app, _ ->
            WsTestClient(app, "/?exception=true").connectAndDisconnect()
            Assertions.assertThat(log).hasSize(1)
        }
    }

    @Test
    fun `websocket subprotocol is set if included`() = TestUtil.test { app, http ->
        app.unsafe.routes.ws("/ws") {}
        val response = Unirest.get("http://localhost:${app.port()}/ws")
            .header(Header.SEC_WEBSOCKET_KEY, "not-null")
            .header(WebSocketConstants.SEC_WEBSOCKET_PROTOCOL, "mqtt")
            .asString()
        Assertions.assertThat(response.headers.getFirst(WebSocketConstants.SEC_WEBSOCKET_PROTOCOL)).isEqualTo("mqtt")
    }

    @Test
    fun `websocket closeSession() methods`() {
        val scenarios = mapOf(
            { client: WsTestClient -> client.send("NO_ARGS") } to WsCloseStatus.NORMAL_CLOSURE,
            { client: WsTestClient -> client.send("STATUS_OBJECT") } to WsCloseStatus.GOING_AWAY,
            { client: WsTestClient -> client.send("CODE_AND_REASON") } to WsCloseStatus.PROTOCOL_ERROR,
            { client: WsTestClient -> client.send("CLOSE_STATUS") } to WsCloseStatus.RESERVED,
            { client: WsTestClient -> client.send("UNEXPECTED") } to WsCloseStatus.RESERVED
        )

        scenarios.forEach { (sendAction, closeStatus) ->
            TestUtil.test { app, _ ->
                app.unsafe.routes.ws("/websocket") { ws ->
                    ws.onMessage { ctx ->
                        when (ctx.message()) {
                            "NO_ARGS" -> ctx.closeSession()
                            "STATUS_OBJECT" -> ctx.closeSession(WsCloseStatus.GOING_AWAY)
                            "CODE_AND_REASON" -> ctx.closeSession(1002, "CODE_AND_REASON")
                            "CLOSE_STATUS" -> ctx.closeSession(WsCloseStatus.RESERVED)
                            else -> ctx.closeSession(1004, "UNEXPECTED")
                        }
                    }
                    ws.onClose { ctx ->
                        // Check the close status code matches expected
                        Assertions.assertThat(ctx.status()).isEqualTo(closeStatus.code)
                        Assertions.assertThat(ctx.closeStatus().code).isEqualTo(closeStatus.code)
                    }
                }
                WsTestClient(app, "/websocket", onOpen = { sendAction(it) }).connectBlocking()
            }
        }
    }

    private fun pingingApp(log: ConcurrentLinkedQueue<String>) = Javalin.create {
        it.routes.ws("/ws") { ws ->
            ws.onConnect { it.enableAutomaticPings(5, TimeUnit.MILLISECONDS) }
            ws.onMessage {
                it.disableAutomaticPings()
                log.clear()
            }
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    fun `websocket enableAutomaticPings() works`() {
        val log = ConcurrentLinkedQueue<String>()
        TestUtil.test(pingingApp(log)) { app, _ ->
            val client = WsTestClient(app, "/ws", onPing = { frame -> log.add(frame!!.payloadData.get(2).toString()) })
            client.connectBlocking()
            Thread.sleep(50)
            Assertions.assertThat(log.size).isGreaterThan(0)
            client.disconnectBlocking()
        }
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    fun `websocket disableAutomaticPings() works`() {
        val log = ConcurrentLinkedQueue<String>()
        TestUtil.test(pingingApp(log)) { app, _ ->
            val client = WsTestClient(app, "/ws", onPing = { frame -> log.add(frame!!.payloadData.get(2).toString()) })
            client.connectBlocking()
            Thread.sleep(50)
            Assertions.assertThat(log.size).isGreaterThan(0)
            awaitCondition(condition = { PingManager.pingFutures.isEmpty() }) { client.send("DISABLE_PINGS") }
            Thread.sleep(50)
            Assertions.assertThat(log.size).isEqualTo(0) // no pings sent during sleep after disabling pings
            client.disconnectBlocking()
        }
    }

    @Test
    fun `websocket idle timeout works`() {
        val log = ConcurrentLinkedQueue<String>()
        TestUtil.test(Javalin.create {
            it.jetty.modifyWebSocketServletFactory {
                it.idleTimeout = Duration.ofMillis(10L)
            }
        }) { app, _ ->
            app.unsafe.routes.ws("/ws") { ws ->
                ws.onClose { log.add("Closed due to timeout") }
            }
            val client = WsTestClient(app, "/ws")
            client.connectBlocking()
            Thread.sleep(50) // Wait longer than the timeout to provoke it
            Assertions.assertThat(log).contains("Closed due to timeout")
        }
    }
}
