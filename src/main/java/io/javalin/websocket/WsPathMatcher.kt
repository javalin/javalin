/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.core.PathParser
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.UpgradeRequest
import org.eclipse.jetty.websocket.api.annotations.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class WsEntry(val path: String, val handler: WsHandler, val caseSensitiveUrls: Boolean) {
    private val pathParser = PathParser(path, caseSensitiveUrls)
    fun matches(requestUri: String) = pathParser.matches(requestUri)
    fun extractPathParams(requestUri: String) = pathParser.extractPathParams(requestUri)
}

/**
 * Every WebSocket request passes through a single instance of this class.
 * Session IDs are generated and tracked here, and path-parameters are cached for performance.
 */
@WebSocket
class WsPathMatcher {

    val wsEntries = mutableListOf<WsEntry>()
    private val sessionIds = ConcurrentHashMap<Session, String>()
    private val sessionPathParams = ConcurrentHashMap<Session, Map<String, String>>()

    fun add(wsEntry: WsEntry) {
        if (!wsEntry.caseSensitiveUrls && wsEntry.path != wsEntry.path.toLowerCase()) {
            throw IllegalArgumentException("By default URLs must be lowercase. Change casing or call `app.enableCaseSensitiveUrls()` to allow mixed casing.")
        }
        wsEntries.add(wsEntry)
    }

    @OnWebSocketConnect
    fun webSocketConnect(session: Session) {
        findEntry(session)?.let { it.handler.connectHandler?.handle(wrap(session, it)) }
    }

    @OnWebSocketMessage
    fun webSocketMessage(session: Session, message: String) {
        findEntry(session)?.let { it.handler.messageHandler?.handle(wrap(session, it), message) }
    }

    @OnWebSocketMessage
    fun webSocketBinaryMessage(session: Session, buffer: ByteArray, offset: Int, length: Int) {
        findEntry(session)?.let { it.handler.binaryMessageHandler?.handle(wrap(session, it), buffer.toTypedArray(), offset, length) }
    }

    @OnWebSocketError
    fun webSocketError(session: Session, throwable: Throwable?) {
        findEntry(session)?.let { it.handler.errorHandler?.handle(wrap(session, it), throwable) }
    }

    @OnWebSocketClose
    fun webSocketClose(session: Session, statusCode: Int, reason: String?) {
        findEntry(session)?.let { it.handler.closeHandler?.handle(wrap(session, it), statusCode, reason) }
        destroy(session)
    }

    private fun findEntry(session: Session) = findEntry(session.upgradeRequest)

    fun findEntry(req: UpgradeRequest) = wsEntries.find { it.matches(req.requestURI.path) }

    private fun wrap(session: Session, wsEntry: WsEntry): WsSession {
        sessionIds.putIfAbsent(session, UUID.randomUUID().toString())
        sessionPathParams.putIfAbsent(session, wsEntry.extractPathParams(session.upgradeRequest.requestURI.path))
        return WsSession(sessionIds[session]!!, session, sessionPathParams[session]!!, wsEntry.path)
    }

    private fun destroy(session: Session) {
        sessionIds.remove(session)
        sessionPathParams.remove(session)
    }

}
