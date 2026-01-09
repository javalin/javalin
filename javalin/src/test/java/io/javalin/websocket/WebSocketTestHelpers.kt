/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import org.java_websocket.WebSocket
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.framing.Framedata
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeoutException

/**
 * A simple WebSocket test client that wraps java-websocket's WebSocketClient.
 */
open class WsTestClient(
    app: Javalin,
    path: String,
    headers: Map<String, String> = emptyMap(),
    val onOpen: (WsTestClient) -> Unit = {},
    var onMessage: ((String) -> Unit)? = null,
    var onPing: ((Framedata?) -> Unit)? = null,
    var onPong: ((Framedata?) -> Unit)? = null,
) : WebSocketClient(URI.create("ws://localhost:" + app.port() + path), Draft_6455(), headers, 0) {

    override fun onOpen(serverHandshake: ServerHandshake) = onOpen(this)
    override fun onClose(status: Int, message: String, byRemote: Boolean) {}
    override fun onError(exception: Exception) {}
    override fun onMessage(message: String) {
        onMessage?.invoke(message)
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
        disconnectBlocking()
    }

    fun connectSendAndDisconnect(message: String) {
        connectBlocking()
        send(message)
        disconnectBlocking()
    }

    fun disconnectBlocking() {
        closeBlocking()
        Thread.sleep(1) // ensure other threads finish processing
    }
}

/**
 * Executes [action] then waits until [condition] returns true, or throws after [timeout].
 */
fun awaitCondition(
    timeout: Duration = Duration.ofSeconds(1),
    pollInterval: Duration = Duration.ofMillis(10),
    condition: () -> Boolean,
    action: () -> Unit = {}
) {
    action()
    val deadline = System.currentTimeMillis() + timeout.toMillis()
    while (!condition()) {
        if (System.currentTimeMillis() > deadline) {
            throw TimeoutException("Condition not met within $timeout")
        }
        Thread.sleep(pollInterval.toMillis())
    }
}

/**
 * Creates a Javalin instance with context path "/websocket".
 */
fun contextPathJavalin(cfg: ((JavalinConfig) -> Unit)? = null): Javalin =
    Javalin.create {
        it.router.contextPath = "/websocket"
        cfg?.invoke(it)
    }

