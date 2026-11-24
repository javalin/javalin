/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.testing.HttpUtil
import io.javalin.testing.TestUtil
import org.assertj.core.api.Assertions.assertThat
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.client.Request
import org.eclipse.jetty.client.Response
import org.eclipse.jetty.client.transport.HttpClientConnectionFactory
import org.eclipse.jetty.client.transport.HttpClientTransportDynamic
import org.eclipse.jetty.http.HttpFields
import org.eclipse.jetty.http.HttpVersion
import org.eclipse.jetty.http2.client.HTTP2Client
import org.eclipse.jetty.http2.client.transport.ClientConnectionFactoryOverHTTP2
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
import org.eclipse.jetty.io.ClientConnector
import org.eclipse.jetty.server.HttpConnectionFactory
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.util.BufferUtil
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.api.Callback
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.StatusCode
import org.eclipse.jetty.websocket.api.UpgradeResponse
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.JettyUpgradeListener
import org.eclipse.jetty.websocket.client.WebSocketClient
import java.net.URI
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.function.Consumer
import kotlin.concurrent.withLock

/*
 * Every websocket test has the same shape:
 * 1. Arrange: routes are set up in config, and handlers only call record(...)
 * 2. Act: the test drives a real WsClient over a real socket
 * 3. Await: the test waits for recorded events (server.expect, client.nextMessage, ...)
 * 4. Assert: always on the test thread, never inside a handler
 * No Thread.sleep: every wait is for a specific event, with a timeout.
 */

val WS_TEST_TIMEOUT: Duration = Duration.ofSeconds(2)

/** Runs [test] against a started app. With [http2], clients connect over TLS and negotiate HTTP/2 when possible. */
fun wsTest(config: WsTest.(JavalinConfig) -> Unit = {}, http2: Boolean = false, test: WsTest.() -> Unit) {
    val harness = WsTest(http2)
    val app = Javalin.create {
        if (http2) it.addHttp2Connectors()
        harness.config(it)
    }
    TestUtil.test(app) { startedApp, http -> harness.run(startedApp, http, test) }
}

class WsTest internal constructor(private val http2: Boolean) {

    val server = EventLog("server")
    lateinit var app: Javalin
        private set
    lateinit var http: HttpUtil
        private set

    private val jettyClient = (if (http2) http2WebSocketClient() else WebSocketClient()).apply {
        httpClient.isFollowRedirects = false
        maxBinaryMessageSize = 16 * 1024 * 1024
    }
    private val clients = mutableListOf<WsClient>()

    fun record(event: String) = server.add(event)

    /** Records "tag:<event>" for every event of a ws, wsBefore or wsAfter handler. */
    fun recorder(tag: String) = Consumer<WsConfig> { ws ->
        ws.onConnect { record("$tag:connect") }
        ws.onMessage { ctx -> record("$tag:message:${ctx.message()}") }
        ws.onBinaryMessage { ctx -> record("$tag:binary:${ctx.data().remaining()}") }
        ws.onPing { ctx -> record("$tag:ping:${ctx.data().remaining()}") }
        ws.onPong { ctx -> record("$tag:pong:${ctx.data().remaining()}") }
        ws.onClose { ctx -> record("$tag:close:${ctx.status()}") }
    }

    /** Connects a client. With [holdReading], the client stops reading when the first text message arrives, until [WsClient.resumeReading]. */
    fun connect(path: String, headers: Map<String, String> = emptyMap(), subProtocols: List<String> = emptyList(), holdReading: Boolean = false): WsClient {
        val client = WsClient(holdReading)
        val request = ClientUpgradeRequest(uri(path)).apply {
            headers.forEach { (name, value) -> setHeader(name, value) }
            if (subProtocols.isNotEmpty()) setSubProtocols(subProtocols)
        }
        val handshake = HandshakeCapture()
        try {
            jettyClient.connect(client, request, handshake).get(WS_TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        } catch (e: Exception) {
            throw AssertionError("Expected upgrade of '$path' to succeed, got ${handshake.response?.status}", e)
        }
        client.httpVersion = handshake.response!!.version
        return client.also { clients += it }
    }

    data class RejectedUpgrade(val status: Int, val headers: HttpFields)

    /** Attempts an upgrade that the server should refuse, and returns the refusing response. */
    fun expectRejectedUpgrade(path: String): RejectedUpgrade {
        val handshake = HandshakeCapture()
        val failure = runCatching {
            jettyClient.connect(WsClient(), ClientUpgradeRequest(uri(path)), handshake).get(WS_TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        }.exceptionOrNull()
        val response = handshake.response
        if (failure == null || response == null) throw AssertionError("Expected upgrade of '$path' to be rejected", failure)
        return RejectedUpgrade(response.status, response.headers)
    }

    private fun uri(path: String): URI = when {
        http2 -> URI.create("wss://localhost:${(app.jettyServer().server().connectors[1] as ServerConnector).localPort}$path")
        else -> URI.create("ws://localhost:${app.port()}$path")
    }

    internal fun run(app: Javalin, http: HttpUtil, test: WsTest.() -> Unit) {
        this.app = app
        this.http = http
        jettyClient.start()
        try {
            test()
        } finally {
            clients.forEach { it.resumeReading() }
            clients.filter { it.isOpen }.forEach { it.session.close() }
            jettyClient.stop()
        }
    }

    private class HandshakeCapture : JettyUpgradeListener {
        @Volatile
        var response: Response? = null

        override fun onHandshakeResponse(request: Request, response: Response) {
            this.response = response
        }
    }
}

// a plain connector comes first so that app.port() keeps serving plain HTTP
private fun JavalinConfig.addHttp2Connectors() {
    jetty.addConnector { server, httpConfig -> ServerConnector(server, HttpConnectionFactory(httpConfig)) }
    jetty.addConnector { server, httpConfig ->
        httpConfig.addCustomizer(SecureRequestCustomizer(false))
        val http11 = HttpConnectionFactory(httpConfig)
        val alpn = ALPNServerConnectionFactory().apply { defaultProtocol = http11.protocol }
        val sslContextFactory = SslContextFactory.Server().apply {
            keyStorePath = WsTest::class.java.getResource("/keystore.jks")!!.toExternalForm()
            keyStorePassword = "localhost"
        }
        ServerConnector(server, SslConnectionFactory(sslContextFactory, alpn.protocol), alpn, HTTP2ServerConnectionFactory(httpConfig), http11)
    }
}

private fun http2WebSocketClient(): WebSocketClient {
    val clientConnector = ClientConnector().apply { sslContextFactory = SslContextFactory.Client(true) }
    val h2 = ClientConnectionFactoryOverHTTP2.HTTP2(HTTP2Client(clientConnector))
    return WebSocketClient(HttpClient(HttpClientTransportDynamic(clientConnector, h2, HttpClientConnectionFactory.HTTP11)))
}

/** Events recorded from server-side handlers, in the order they happened. */
class EventLog(private val name: String) {

    private val events = mutableListOf<String>()
    private val lock = ReentrantLock()
    private val changed = lock.newCondition()

    fun add(event: String) = lock.withLock {
        events += event
        changed.signalAll()
    }

    /** Waits until [count] events have been recorded, then returns all events recorded so far. */
    fun await(count: Int): List<String> = lock.withLock {
        var remaining = WS_TEST_TIMEOUT.toNanos()
        while (events.size < count) {
            if (remaining <= 0) throw AssertionError("Expected $count $name events within $WS_TEST_TIMEOUT, got $events")
            remaining = changed.awaitNanos(remaining)
        }
        events.toList()
    }

    fun expect(vararg expected: String) {
        assertThat(await(expected.size)).containsExactly(*expected)
    }

    /** Asserts that nothing has been recorded so far. */
    fun expectNothing() {
        assertThat(await(0)).isEmpty()
    }

    /** Asserts that no new event is recorded [within] the given duration. */
    fun expectNoMore(within: Duration) = lock.withLock {
        val recorded = events.toList()
        var remaining = within.toNanos()
        while (remaining > 0) remaining = changed.awaitNanos(remaining)
        if (events != recorded) throw AssertionError("Expected no new $name events within $within, got ${events.drop(recorded.size)}")
    }

    fun expectInAnyOrder(vararg expected: String) {
        assertThat(await(expected.size)).containsExactlyInAnyOrder(*expected)
    }
}

/** A real websocket client that records everything it receives, read with blocking calls. */
class WsClient internal constructor(private val holdReading: Boolean = false) : Session.Listener.Abstract() {

    sealed interface Event
    data class Text(val text: String) : Event
    class Binary(val bytes: ByteArray) : Event {
        override fun toString() = "Binary(${bytes.size} bytes)"
    }
    data class Close(val code: Int, val reason: String?) : Event
    data class Error(val error: Throwable) : Event

    private val events = LinkedBlockingQueue<Event>()
    private val pings = LinkedBlockingQueue<ByteArray>()
    private val pongs = LinkedBlockingQueue<ByteArray>()
    private val readingHeld = CountDownLatch(1)
    private val readingResumed = CountDownLatch(1)

    val upgradeResponse: UpgradeResponse get() = session.upgradeResponse
    var httpVersion: HttpVersion? = null
        internal set

    fun send(message: String) = session.sendText(message, Callback.NOOP)
    fun send(bytes: ByteArray) = session.sendBinary(ByteBuffer.wrap(bytes), Callback.NOOP)
    fun sendPing(payload: ByteArray = byteArrayOf()) = session.sendPing(ByteBuffer.wrap(payload), Callback.NOOP)
    fun sendPong(payload: ByteArray = byteArrayOf()) = session.sendPong(ByteBuffer.wrap(payload), Callback.NOOP)

    /** With holdReading, waits until the client has stopped reading. */
    fun awaitReadingHeld() {
        if (!readingHeld.await(WS_TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) throw AssertionError("Expected the client to hold reading within $WS_TEST_TIMEOUT")
    }

    fun resumeReading() {
        if (readingResumed.count > 0) {
            readingResumed.countDown()
            if (readingHeld.count == 0L) session.demand()
        }
    }

    /** Closes the connection and waits for the close handshake to complete. */
    fun close(code: Int = StatusCode.NORMAL, reason: String? = null): Close {
        session.close(code, reason, Callback.NOOP)
        return awaitClose()
    }

    fun nextMessage(): String = when (val event = next()) {
        is Text -> event.text
        else -> throw AssertionError("Expected a text message, got $event")
    }

    fun nextBinary(): ByteArray = when (val event = next()) {
        is Binary -> event.bytes
        else -> throw AssertionError("Expected a binary message, got $event")
    }

    /** Waits for the connection to close, skipping any messages received before the close. */
    fun awaitClose(): Close {
        val skipped = mutableListOf<Event>()
        while (true) {
            when (val event = events.poll(WS_TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)) {
                is Close -> return event
                null -> throw AssertionError("Expected the connection to close within $WS_TEST_TIMEOUT, got $skipped")
                else -> skipped += event
            }
        }
    }

    fun nextPing(): ByteArray = pings.poll(WS_TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        ?: throw AssertionError("Expected a ping within $WS_TEST_TIMEOUT")

    fun nextPong(): ByteArray = pongs.poll(WS_TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        ?: throw AssertionError("Expected a pong within $WS_TEST_TIMEOUT")

    /** Discards pings received so far, then asserts that no new ping arrives [within] the given duration. */
    fun expectNoMorePings(within: Duration) {
        pings.clear()
        val ping = pings.poll(within.toMillis(), TimeUnit.MILLISECONDS)
        if (ping != null) throw AssertionError("Expected no pings within $within, but got one")
    }

    private fun next(): Event = events.poll(WS_TEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS)
        ?: throw AssertionError("Expected an event within $WS_TEST_TIMEOUT, got nothing")

    override fun onWebSocketOpen(session: Session) {
        super.onWebSocketOpen(session)
        session.demand()
    }

    override fun onWebSocketText(message: String) {
        events += Text(message)
        if (holdReading && readingHeld.count > 0 && readingResumed.count > 0) readingHeld.countDown() // stop demanding until resumeReading()
        else session.demand()
    }

    override fun onWebSocketBinary(payload: ByteBuffer, callback: Callback) {
        events += Binary(BufferUtil.toArray(payload))
        callback.succeed()
        session.demand()
    }

    override fun onWebSocketPing(payload: ByteBuffer) {
        pings += BufferUtil.toArray(payload)
        session.demand()
    }

    override fun onWebSocketPong(payload: ByteBuffer) {
        pongs += BufferUtil.toArray(payload)
        session.demand()
    }

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        events += Close(statusCode, reason)
    }

    override fun onWebSocketError(cause: Throwable) {
        events += Error(cause)
    }
}
