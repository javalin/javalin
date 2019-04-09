/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.core.JavalinConfig
import io.javalin.core.PathParser
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.UpgradeRequest
import org.eclipse.jetty.websocket.api.annotations.*
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class WsEntry(val path: String, val handler: WsHandler) {
    private val pathParser = PathParser(path)
    fun matches(requestUri: String) = pathParser.matches(requestUri)
    fun extractPathParams(requestUri: String) = pathParser.extractPathParams(requestUri)
}

/**
 * Every WebSocket request passes through a single instance of this class.
 * Session IDs are generated and tracked here, and path-parameters are cached for performance.
 */
@WebSocket
class WsPathMatcher(val config: JavalinConfig) {

    val wsEntries = mutableListOf<WsEntry>()
    private val sessionIds = ConcurrentHashMap<Session, String>()
    private val sessionPathParams = ConcurrentHashMap<Session, Map<String, String>>()

    fun add(wsEntry: WsEntry) {
        wsEntries.add(wsEntry)
    }

    @OnWebSocketConnect
    fun webSocketConnect(session: Session) {
        findEntry(session)?.let {
            val ctx = WsConnectContext(sessionIds[session]!!, session, sessionPathParams[session]!!, it.path)
            it.handler.connectHandler?.handleConnect(ctx)
            config.wsLogger?.connectHandler?.handleConnect(ctx)
        }

    }

    @OnWebSocketMessage
    fun webSocketMessage(session: Session, message: String) {
        findEntry(session)?.let {
            val ctx = WsMessageContext(sessionIds[session]!!, session, sessionPathParams[session]!!, it.path, message)
            it.handler.messageHandler?.handleMessage(ctx)
            config.wsLogger?.messageHandler?.handleMessage(ctx)
        }
    }

    @OnWebSocketMessage
    fun webSocketBinaryMessage(session: Session, buffer: ByteArray, offset: Int, length: Int) {
        findEntry(session)?.let {
            val ctx = WsBinaryMessageContext(sessionIds[session]!!, session, sessionPathParams[session]!!, it.path, buffer.toTypedArray(), offset, length)
            it.handler.binaryMessageHandler?.handleBinaryMessage(ctx)
            config.wsLogger?.binaryMessageHandler?.handleBinaryMessage(ctx)
        }
    }

    @OnWebSocketError
    fun webSocketError(session: Session, throwable: Throwable?) {
        findEntry(session)?.let {
            val ctx = WsErrorContext(sessionIds[session]!!, session, sessionPathParams[session]!!, it.path, throwable)
            it.handler.errorHandler?.handleError(ctx)
            config.wsLogger?.errorHandler?.handleError(ctx)
        }
    }

    @OnWebSocketClose
    fun webSocketClose(session: Session, statusCode: Int, reason: String?) {
        findEntry(session)?.let {
            val ctx = WsCloseContext(sessionIds[session]!!, session, sessionPathParams[session]!!, it.path, statusCode, reason)
            it.handler.closeHandler?.handleClose(ctx)
            config.wsLogger?.closeHandler?.handleClose(ctx)
        }
        destroy(session)
    }

    private fun findEntry(session: Session) = findEntry(session.upgradeRequest)?.also { cacheSession(session, it) }

    fun findEntry(req: UpgradeRequest) = wsEntries.find { it.matches(req.uriNoContextPath()) }

    private fun cacheSession(session: Session, wsEntry: WsEntry) {
        sessionIds.putIfAbsent(session, UUID.randomUUID().toString())
        sessionPathParams.putIfAbsent(session, wsEntry.extractPathParams(session.upgradeRequest.uriNoContextPath()))
    }

    private fun destroy(session: Session) {
        sessionIds.remove(session)
        sessionPathParams.remove(session)
    }

    private fun UpgradeRequest.uriNoContextPath() = this.requestURI.path.removePrefix((this as ServletUpgradeRequest).httpServletRequest.contextPath)

}
