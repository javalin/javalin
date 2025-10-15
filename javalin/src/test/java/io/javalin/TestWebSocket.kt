/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import io.javalin.apibuilder.ApiBuilder.ws
import io.javalin.config.JavalinConfig
import io.javalin.config.Key
import io.javalin.http.Header
import io.javalin.http.HttpStatus
import io.javalin.http.UnauthorizedResponse
import io.javalin.json.toJsonString
import io.javalin.plugin.bundled.DevLoggingPlugin
import io.javalin.security.RouteRole
import io.javalin.testing.SerializableObject
import io.javalin.testing.TestUtil
import io.javalin.testing.TypedException
import io.javalin.testing.fasterJacksonMapper
import io.javalin.testing.*
import io.javalin.websocket.WsCloseStatus
import io.javalin.websocket.WsContext
import io.javalin.websocket.pingFutures
import kong.unirest.Unirest
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.util.BufferUtil

import org.eclipse.jetty.websocket.api.StatusCode
import org.eclipse.jetty.websocket.api.exceptions.MessageTooLargeException
import org.eclipse.jetty.websocket.api.util.WebSocketConstants
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.framing.Framedata
import org.java_websocket.handshake.ServerHandshake
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.net.URI
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * This test could be better
 */
@Timeout(value = 2, unit = TimeUnit.SECONDS)
class TestWebSocket {

    private data class TestLogger(val log: ConcurrentLinkedQueue<String> = ConcurrentLinkedQueue<String>())
    private val testLoggerKey = Key<TestLogger>("test-logger")

    private fun Javalin.logger(): TestLogger {
        val logger = TestLogger()
        val componentManager = this.unsafeConfig().pvt.appDataManager
        componentManager.registerResolverIfAbsent(testLoggerKey, logger)
        return componentManager.get(testLoggerKey)
    }

    private fun contextPathJavalin(cfg: ((JavalinConfig) -> Unit)? = null): Javalin =
        Javalin.create {
            it.router.contextPath = "/websocket"
            cfg?.invoke(it)
        }

    @Test
    fun `each connection receives a unique id`() {
        val logger = TestLogger()

        TestUtil.test(contextPathJavalin { cfg ->
            cfg.routes.ws("/test-websocket-1") { ws ->
                ws.onConnect { ctx -> logger.log.add(ctx.sessionId()) }
                ws.onClose { ctx -> logger.log.add(ctx.sessionId()) }
            }
            cfg.routes.apiBuilder {
                ws("/test-websocket-2") { ws ->
                    ws.onConnect { ctx -> logger.log.add(ctx.sessionId()) }
                    ws.onClose { ctx -> logger.log.add(ctx.sessionId()) }
                }
            }
        }) { app, _ ->
            TestClient(app, "/websocket/test-websocket-1").connectAndDisconnect()
            TestClient(app, "/websocket/test-websocket-1").connectAndDisconnect()
            TestClient(app, "/websocket/test-websocket-2").connectAndDisconnect()
            // 3 clients and 6 operations should yield 3 unique identifiers total
            val uniqueLog = HashSet(logger.log)
            assertThat(uniqueLog).hasSize(3)
            uniqueLog.forEach { id -> assertThat(uniqueLog.count { it == id }).isEqualTo(1) }
        }
    }

    @Test
    fun `general integration test`() {
        val logger = TestLogger()
        val idMap = ConcurrentHashMap<WsContext, Int>()
        val atomicInteger = AtomicInteger()

        TestUtil.test(contextPathJavalin { cfg ->
            cfg.routes.ws("/test-websocket-1") { ws ->
                    ws.onConnect { ctx ->
                        idMap[ctx] = atomicInteger.getAndIncrement()
                        logger.log.add("${idMap[ctx]} connected")
                    }
                    ws.onMessage { ctx ->
                        logger.log.add("${idMap[ctx]} sent '${ctx.message()}' to server")
                        idMap.forEach { (client, _) -> client.send("Server sent '${ctx.message()}' to ${idMap[client]}") }
                    }
                    ws.onClose { ctx -> logger.log.add("${idMap[ctx]} disconnected") }
                }
            cfg.routes.apiBuilder { // use .routes to test apibuilder
                ws("/test-websocket-2") { ws ->
                    ws.onConnect { logger.log.add("Connected to other endpoint") }
                    ws.onClose { logger.log.add("Disconnected from other endpoint") }
                }
            }
        }) { app, _ ->
            val testClient0 = TestClient(app, "/websocket/test-websocket-1", logger = logger).also { it.connectBlocking() } // logsize = 1
            val testClient1 = TestClient(app, "/websocket/test-websocket-1", logger = logger).also { it.connectBlocking() } // logsize = 2
            doBlocking({
                testClient0.send("A") // logsize = 3 (this will add +2 to logsize when clients register echo)
                testClient1.send("B") // logsize = 4 (this will add +2 to logsize when clients register echo)
            }, { logger.log.size != 8 }) // // logsize = 8 (block until all echos registered)
            testClient0.closeBlocking()
            testClient1.closeBlocking()
            TestClient(app, "/websocket/test-websocket-2", logger = logger).also { it.connectAndDisconnect() }
            assertThat(logger.log).containsExactlyInAnyOrder(
                "0 connected",
                "1 connected",
                "0 sent 'A' to server",
                "1 sent 'B' to server",
                "Server sent 'A' to 0",
                "Server sent 'A' to 1",
                "Server sent 'B' to 0",
                "Server sent 'B' to 1",
                "0 disconnected",
                "1 disconnected",
                "Connected to other endpoint",
                "Disconnected from other endpoint"
            )
        }
    }

    @Test
    fun `receive and send json messages`() = TestUtil.test(Javalin.create {
        it.jsonMapper(fasterJacksonMapper)
    }) { app, _ ->
        app.ws("/message") { ws ->
            ws.onMessage { ctx ->
                val receivedMessage = ctx.messageAsClass<SerializableObject>()
                receivedMessage.value1 = "updated"
                ctx.send(receivedMessage)
            }
        }

        val clientJsonString = fasterJacksonMapper.toJsonString(SerializableObject().apply { value1 = "test1"; value2 = "test2" })
        var response: String? = null
        val testClient = TestClient(app, "/message").also {
            it.onMessage = { msg -> response = msg }
            it.connectBlocking()
        }
        doBlocking({ testClient.send(clientJsonString) }, { response == null }) // have to wait for client to receive response
        assertThat(response).contains(""""value1":"updated"""")
        assertThat(response).contains(""""value2":"test2"""")
    }

    @Test
    fun `binary messages`() = TestUtil.test(contextPathJavalin()) { app, _ ->
        val byteDataToSend1 = (0 until 4096).shuffled().map { it.toByte() }.toByteArray()
        val byteDataToSend2 = (0 until 4096).shuffled().map { it.toByte() }.toByteArray()
        val receivedBinaryData = mutableListOf<ByteArray>()
        app.ws("/binary") { ws ->
            ws.onBinaryMessage { ctx ->
                val data = BufferUtil.toArray(ctx.data())
                receivedBinaryData.add(data)
            }
        }
        TestClient(app, "/websocket/binary").also {
            it.connectBlocking()
            it.send(byteDataToSend1)
            it.send(byteDataToSend2)
            it.closeBlocking()
        }
        assertThat(receivedBinaryData).containsExactlyInAnyOrder(byteDataToSend1, byteDataToSend2)
    }

    @Test
    fun `routing and pathParams work without context path`() = TestUtil.test { app, _ ->
        app.ws("/params/{1}") { ws -> ws.onConnect { ctx -> app.logger().log.add(ctx.pathParam("1")) } }
        app.ws("/params/{1}/test/{2}/{3}") { ws -> ws.onConnect { ctx -> app.logger().log.add(ctx.pathParam("1") + " " + ctx.pathParam("2") + " " + ctx.pathParam("3")) } }
        app.ws("/*") { ws -> ws.onConnect { _ -> app.logger().log.add("catchall") } } // this should not be triggered since all calls match more specific handlers
        TestClient(app, "/params/one").connectAndDisconnect()
        TestClient(app, "/params/%E2%99%94").connectAndDisconnect()
        TestClient(app, "/params/another/test/long/path").connectAndDisconnect()
        assertThat(app.logger().log).containsExactlyInAnyOrder("one", "♔", "another long path")
        assertThat(app.logger().log).doesNotContain("catchall")
    }

    @Test
    fun `routing and pathParams work with context path`() = TestUtil.test(contextPathJavalin()) { app, _ ->
        app.ws("/params/{1}") { ws -> ws.onConnect { ctx -> app.logger().log.add(ctx.pathParam("1")) } }
        app.ws("/params/{1}/test/{2}/{3}") { ws -> ws.onConnect { ctx -> app.logger().log.add(ctx.pathParam("1") + " " + ctx.pathParam("2") + " " + ctx.pathParam("3")) } }
        app.ws("/*") { ws -> ws.onConnect { _ -> app.logger().log.add("catchall") } } // this should not be triggered since all calls match more specific handlers
        TestClient(app, "/websocket/params/one").connectAndDisconnect()
        TestClient(app, "/websocket/params/%E2%99%94").connectAndDisconnect()
        TestClient(app, "/websocket/params/another/test/long/path").connectAndDisconnect()
        assertThat(app.logger().log).containsExactlyInAnyOrder("one", "♔", "another long path")
        assertThat(app.logger().log).doesNotContain("catchall")
    }

    @Test
    fun `context-path vs no context-path app`() {
        val noContextPathApp = Javalin.create().apply {
            this.ws("/p/{id}") { it.onConnect { this.logger().log.add(it.pathParam("id")) } }
        }.start(0)
        val contextPathApp = Javalin.create { it.router.contextPath = "/websocket" }.apply {
            this.ws("/p/{id}") { it.onConnect { this.logger().log.add(it.pathParam("id")) } }
        }.start(0)
        TestClient(noContextPathApp, "/p/ncpa").connectAndDisconnect()
        TestClient(contextPathApp, "/websocket/p/cpa").connectAndDisconnect()
        assertThat(noContextPathApp.logger().log).containsExactly("ncpa")
        assertThat(contextPathApp.logger().log).containsExactly("cpa")
        noContextPathApp.stop()
        contextPathApp.stop()
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
            ws.onClose { ctx -> app.logger().log.add("Closed connection from: " + ctx.host()) }
        }
        TestClient(app, "/websocket", mapOf("Test" to "HeaderParameter")).connectAndDisconnect()
        assertThat(app.logger().log).containsExactlyInAnyOrder("Header: HeaderParameter", "Closed connection from: localhost:${app.port()}")
    }

    @Test
    fun `extracting path information works`() = TestUtil.test { app, _ ->
        var matchedPath = ""
        var pathParam = ""
        var queryParam = ""
        var queryParams = listOf<String>()
        app.ws("/websocket/{channel}") { ws ->
            ws.onConnect { ctx ->
                matchedPath = ctx.matchedPath()
                pathParam = ctx.pathParam("channel")
                queryParam = ctx.queryParam("qp")!!
                queryParams = ctx.queryParams("qps")
            }
        }
        TestClient(app, "/websocket/channel-one?qp=just-a-qp&qps=1&qps=2").connectAndDisconnect()
        assertThat(matchedPath).isEqualTo("/websocket/{channel}")
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
        TestClient(app, "/context-life?qp=great").connectSendAndDisconnect("not-important")
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
        app.get("/") { it.sessionAttribute("session-key", "session-value") }
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
        app.ws("/pAtH/{param}") { ws -> ws.onConnect { ctx -> app.logger().log.add(ctx.pathParam("param")) } }
        app.ws("/other-path/{param}") { ws -> ws.onConnect { ctx -> app.logger().log.add(ctx.pathParam("param")) } }
        TestClient(app, "/PaTh/my-param").connectAndDisconnect()
        assertThat(app.logger().log).doesNotContain("my-param")
        TestClient(app, "/other-path/My-PaRaM").connectAndDisconnect()
        assertThat(app.logger().log).contains("My-PaRaM")
    }

    @Test
    fun `web socket logging works`() = TestUtil.test(Javalin.create().apply {
        this.unsafeConfig().requestLogger.ws { ws ->
            ws.onConnect { ctx -> this.logger().log.add(ctx.pathParam("param") + " connected") }
            ws.onClose { ctx -> this.logger().log.add(ctx.pathParam("param") + " disconnected") }
        }
    }) { app, _ ->
        app.ws("/path/{param}") {}
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
    fun `web socket upgrade logging works`() = TestUtil.test(Javalin.create().apply {
        this.unsafeConfig().requestLogger.ws { ws ->
            ws.onUpgrade { ctx, executionTimeMs ->
                this.logger().log.add("${ctx.path()} upgrade attempted (${ctx.status()})")
            }
        }
    }) { app, _ ->
        app.ws("/upgrade-test") {}
        // Test successful upgrade
        TestClient(app, "/upgrade-test").connectAndDisconnect()
        // Test failed upgrade (404)
        val response = Unirest.get("http://localhost:${app.port()}/non-existent-ws")
            .header(Header.SEC_WEBSOCKET_KEY, "test-key")
            .header(Header.UPGRADE, "websocket")
            .header(Header.CONNECTION, "upgrade")
            .asString()
        assertThat(response.status).isEqualTo(404)

        // Verify that both successful and failed upgrades are logged
        assertThat(app.logger().log).containsExactlyInAnyOrder(
            "/upgrade-test upgrade attempted (101 Switching Protocols)",
            "/non-existent-ws upgrade attempted (404 Not Found)"
        )
    }

    @Test
    fun `web socket upgrade logging works for wsBeforeUpgrade errors`() = TestUtil.test(Javalin.create().apply {
        this.unsafeConfig().requestLogger.ws { ws ->
            ws.onUpgrade { ctx, executionTimeMs ->
                this.logger().log.add("${ctx.path()} upgrade attempted (${ctx.status()})")
            }
        }
    }) { app, _ ->
        app.wsBeforeUpgrade("/auth-ws") { ctx ->
            throw UnauthorizedResponse()
        }
        app.ws("/auth-ws") {}

        // Test failed upgrade due to authentication
        val response = Unirest.get("http://localhost:${app.port()}/auth-ws")
            .header(Header.SEC_WEBSOCKET_KEY, "test-key")
            .header(Header.UPGRADE, "websocket")
            .header(Header.CONNECTION, "upgrade")
            .asString()
        assertThat(response.status).isEqualTo(401)

        // Verify that the failed upgrade due to authentication error is logged
        assertThat(app.logger().log).containsExactly("/auth-ws upgrade attempted (401 Unauthorized)")
    }

    @Test
    fun `dev logging works for web sockets`() = TestUtil.test(Javalin.create { it.registerPlugin(DevLoggingPlugin()) }) { app, _ ->
        app.ws("/path/{param}") {}
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
        var handlerError: Throwable? = null
        val maxTextSize = 1L
        val textToSend = "This text is far too long."
        val app = Javalin.create {
            it.jetty.modifyWebSocketServletFactory { wsFactory ->
                wsFactory.maxTextMessageSize = maxTextSize
            }
        }.ws("/ws") { ws ->
            ws.onError { ctx -> handlerError = ctx.error() }
        }
        TestUtil.test(app) { _, _ ->
            TestClient(app, "/ws").connectSendAndDisconnect(textToSend)
            repeat(10) {
                if (handlerError == null) Thread.sleep(5) // give Javalin time to trigger the error handler
            }
            assertThat(handlerError!!.message).isEqualTo("Text message too large: ${textToSend.length} > $maxTextSize")
            assertThat(handlerError).isExactlyInstanceOf(MessageTooLargeException::class.java)
        }
    }

    private fun accessManagedJavalin(): Javalin = Javalin.create().apply {
        this.wsBeforeUpgrade { ctx ->
            this.logger().log.add("handling upgrade request ...")
            when {
                ctx.queryParam("exception") == "true" -> throw UnauthorizedResponse()
                ctx.queryParam("allowed") == "true" -> {
                    this.logger().log.add("upgrade request valid!")
                    return@wsBeforeUpgrade
                }
                else -> {
                    this.logger().log.add("upgrade request invalid!")
                    ctx.skipRemainingHandlers()
                }
            }
        }
        this.ws("/*") { ws ->
            ws.onConnect { this.logger().log.add("connected with upgrade request") }
        }
    }

    @Test
    fun `AccessManager rejects invalid request`() = TestUtil.test(accessManagedJavalin()) { app, _ ->
        TestClient(app, "/").connectAndDisconnect()
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
        TestClient(app, "/?exception=true").connectAndDisconnect()
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
        TestClient(app, "/ws").connectSendAndDisconnect("test")
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
    fun `throw in wsAfter is covered by wsException`() = TestUtil.test { app, _ ->
        app.wsAfter { it.onConnect { throw UnauthorizedResponse() } }
        app.ws("/ws") { it.onConnect { app.logger().log.add("This should be added") } }
        app.wsException(Exception::class.java) { e, _ -> app.logger().log.add(e.message!!) }
        TestClient(app, "/ws").connectAndDisconnect()
        assertThat(app.logger().log).contains("Unauthorized")
        assertThat(app.logger().log).contains("This should be added")
    }

    @Test
    fun `wsAfter works for onConnect`() = TestUtil.test { app, _ ->
        app.wsAfter { it.onConnect { app.logger().log.add("After!") } }
        app.ws("/ws") { it.onConnect { app.logger().log.add("Endpoint!") } }
        TestClient(app, "/ws").connectAndDisconnect()
        assertThat(app.logger().log).containsExactly("Endpoint!", "After!")
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
        TestClient(app, "/ws").connectSendAndDisconnect("test")
        assertThat(app.logger().log).containsExactly(
            "endpoint handler: onConnect", "after handler: onConnect",
            "endpoint handler: onMessage", "after handler: onMessage",
            "endpoint handler: onClose", "after handler: onClose"
        )
    }

    @Test
    fun `unmapped exceptions are caught by default handler`() = TestUtil.test { app, _ ->
        app.ws("/ws") { it.onConnect { throw Exception("EX") } }
        val client = object : TestClient(app, "/ws") {
            override fun onClose(status: Int, message: String, byRemote: Boolean) {
                this.app.logger().log.add("Status code: $status")
                this.app.logger().log.add("Reason: $message")
            }
        }
        doBlocking({ client.connect() }, { !client.isClosed }) // hmmm
        assertThat(client.app.logger().log).containsExactly(
            "Status code: ${StatusCode.SERVER_ERROR}",
            "Reason: EX"
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

    @Test
    fun `websocket closeSession() methods`() {
        val scenarios = mapOf(
            { client: TestClient -> client.send("NO_ARGS") } to WsCloseStatus.NORMAL_CLOSURE,
            { client: TestClient -> client.send("STATUS_OBJECT") } to WsCloseStatus.GOING_AWAY,
            { client: TestClient -> client.send("CODE_AND_REASON") } to WsCloseStatus.PROTOCOL_ERROR,
            { client: TestClient -> client.send("CLOSE_STATUS") } to WsCloseStatus.RESERVED,
            { client: TestClient -> client.send("UNEXPECTED") } to WsCloseStatus.RESERVED
        )

        scenarios.forEach { (sendAction, closeStatus) ->
            TestUtil.test { app, _ ->
                app.ws("/websocket") { ws ->
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
                        assertThat(ctx.status()).isEqualTo(closeStatus.code)
                        assertThat(ctx.closeStatus().code).isEqualTo(closeStatus.code)
                    }
                }
                TestClient(app, "/websocket", onOpen = { sendAction(it) }).connectBlocking()
            }
        }
    }

    private fun pingingApp() = Javalin.create().apply {
        this.ws("/ws") { ws ->
            ws.onConnect { it.enableAutomaticPings(5, TimeUnit.MILLISECONDS, ByteBuffer.wrap(byteArrayOf(0, 0, 0))) }
            ws.onMessage {
                it.disableAutomaticPings()
                this.logger().log.clear()
            }
        }
    }

    private fun loggingClient(app: Javalin) = TestClient(app, "/ws", emptyMap(), {}, null, { frame: Framedata? ->
        app.logger().log.add(frame!!.payloadData.get(2).toString())
    }, null)

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    fun `websocket enableAutomaticPings() works`() = TestUtil.test(pingingApp()) { app, _ ->
        val client = loggingClient(app)
        client.connectBlocking()
        Thread.sleep(50)
        assertThat(app.logger().log.size).isGreaterThan(0)
        client.disconnectBlocking()
    }

    @Test
    @Timeout(value = 2, unit = TimeUnit.SECONDS)
    fun `websocket disableAutomaticPings() works`() = TestUtil.test(pingingApp()) { app, _ ->
        val client = loggingClient(app)
        client.connectBlocking()
        Thread.sleep(50)
        assertThat(app.logger().log.size).isGreaterThan(0)
        doBlocking({ client.send("DISABLE_PINGS") }, { pingFutures.size > 0 }) // check that this map clears
        Thread.sleep(50)
        assertThat(app.logger().log.size).isEqualTo(0) // no pings sent during sleep after disabling pings
        client.disconnectBlocking()
    }

    @Test
    fun `wsBeforeUpgrade and wsAfterUpgrade are invoked`() = TestUtil.test { app, http ->
        app.wsBeforeUpgrade { app.logger().log.add("before") }
        app.wsAfterUpgrade { app.logger().log.add("after") }
        app.ws("/ws") {}
        http.wsUpgradeRequest("/ws")
        assertThat(app.logger().log).containsExactly("before", "after")
    }

    @Test
    fun `wsBeforeUpgrade and after can modify the upgrade request`() = TestUtil.test { app, http ->
        app.wsBeforeUpgrade { it.header("X-Before", "before") }
        app.wsAfterUpgrade { it.header("X-After", "after") }
        app.ws("/ws") {}
        val response = http.wsUpgradeRequest("/ws")
        assertThat(response.headers.getFirst("X-Before")).isEqualTo("before")
        assertThat(response.headers.getFirst("X-After")).isEqualTo("after")
    }

    @Test
    fun `wsBeforeUpgrade can stop an upgrade request in progress`() = TestUtil.test { app, http ->
        app.wsBeforeUpgrade { _ -> throw IllegalStateException("denied") }
        app.ws("/ws") { ws ->
            ws.onConnect { app.logger().log.add("connected") }
        }
        val response = http.wsUpgradeRequest("/ws")
        assertThat(response.status).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.code)
        assertThat(app.logger().log).isEmpty()
    }

    @Test
    fun `wsBeforeUpgrade exception pattern can be combined with a custom exception handler`() = TestUtil.test { app, http ->
        app.wsBeforeUpgrade { throw IllegalStateException("denied") }
        app.exception(IllegalStateException::class.java) { _, ctx ->
            app.logger().log.add("exception handled")
            ctx.status(HttpStatus.FORBIDDEN)
        }
        app.ws("/ws") {}
        val response = http.wsUpgradeRequest("/ws")
        assertThat(app.logger().log).containsExactly("exception handled")
        assertThat(response.status).isEqualTo(HttpStatus.FORBIDDEN.code)
    }

    @Test
    fun `wsBeforeUpgrade does work with skipRemainingHandlers`() = TestUtil.test { app, http ->
        app.wsBeforeUpgrade { it.status(HttpStatus.FORBIDDEN).skipRemainingHandlers() }
        app.ws("/ws") { ws ->
            ws.onConnect { app.logger().log.add("connected") }
        }
        val client = TestClient(app, "/ws")
        client.connectAndDisconnect()
        val response = http.wsUpgradeRequest("/ws")
        assertThat(response.status).isEqualTo(HttpStatus.FORBIDDEN.code)
        assertThat(app.logger().log).isEmpty()
    }

    @Test
    fun `wsBeforeUpgrade in full lifecycle`() = TestUtil.test { app, _ ->
        app.wsBeforeUpgrade { app.logger().log.add("before-upgrade") }
        app.wsAfterUpgrade { app.logger().log.add("after-upgrade") }
        app.ws("/ws") { ws ->
            ws.onConnect { app.logger().log.add("connect") }
            ws.onMessage { app.logger().log.add("msg") }
            ws.onClose { app.logger().log.add("close") }
        }
        val client = TestClient(app, "/ws")
        client.connectSendAndDisconnect("test-message")
        assertThat(app.logger().log).containsExactly("before-upgrade", "after-upgrade", "connect", "msg", "close")
    }

    private enum class WsRole : RouteRole { A }

    @Test
    fun `routeRoles are available in wsBeforeUpgrade`() = TestUtil.test { app, http ->
        app.wsBeforeUpgrade { app.logger().log.add(it.routeRoles().toString()) }
        app.ws("/ws", {}, WsRole.A)
        http.wsUpgradeRequest("/ws")
        assertThat(app.logger().log).containsExactly("[A]")
    }

    @Test
    fun `pathParams are available in wsBeforeUpgrade`() = TestUtil.test { app, http ->
        app.wsBeforeUpgrade { app.logger().log.add(it.pathParam("param")) }
        app.ws("/ws/{param}") {}
        http.wsUpgradeRequest("/ws/123")
        assertThat(app.logger().log).containsExactly("123")
    }

    // ********************************************************************************************
    // Helpers
    // ********************************************************************************************

    private open class TestClient(
        var app: Javalin,
        path: String,
        headers: Map<String, String> = emptyMap(),
        val onOpen: (TestClient) -> Unit = {},
        var onMessage: ((String) -> Unit)? = null,
        var onPing: ((Framedata?) -> Unit)? = null,
        var onPong: ((Framedata?) -> Unit)? = null,
        val logger: TestLogger = run {
            val testLogger = TestLogger()
            val componentManager = app.unsafeConfig().pvt.appDataManager
            val key = Key<TestLogger>("test-logger")
            componentManager.registerResolverIfAbsent(key, testLogger)
            componentManager.get(key)
        }
        ) : WebSocketClient(URI.create("ws://localhost:" + app.port() + path), Draft_6455(), headers, 0), AutoCloseable {

        override fun onOpen(serverHandshake: ServerHandshake) = onOpen(this)
        override fun onClose(status: Int, message: String, byRemote: Boolean) {}
        override fun onError(exception: Exception) {}
        override fun onMessage(message: String) {
            onMessage?.invoke(message)
            logger.log.add(message)
        }

        override fun onWebsocketPing(conn: WebSocket?, f: Framedata?) {
            super.onWebsocketPing(conn, f)
            onPing?.invoke(f)
        }

        override fun onWebsocketPong(conn: WebSocket?, f: Framedata?) {
            super.onWebsocketPong(conn, f)
            onPong?.invoke(f)
        }

        fun connectAndDisconnect() {
            connectBlocking()
            disconnectBlocking();
        }

        fun connectSendAndDisconnect(message: String) {
            connectBlocking()
            send(message)
            disconnectBlocking();
        }

        fun disconnectBlocking() {
            closeBlocking()
            awaitResponse()
        }

        private fun awaitResponse() = Thread.sleep(1) // freeze this thread, so we're sure that other threads had a chance to finish responses
    }

    private fun doBlocking(slowFunction: () -> Unit, conditionFunction: () -> Boolean, timeout: Duration = Duration.ofSeconds(1)) {
        val startTime = System.currentTimeMillis()
        val limitTime = startTime + timeout.toMillis()
        slowFunction.invoke()
        while (conditionFunction.invoke()) {
            if (System.currentTimeMillis() > limitTime) {
                throw TimeoutException("Wait for condition has timed out")
            }
            Thread.sleep(25)
        }
    }
}
