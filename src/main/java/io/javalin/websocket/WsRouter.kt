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

data class WebSocketEntry(val path: String, val handler: WsHandler) {
    private val parser: PathParser = PathParser(path)
    fun matches(requestUri: String) = parser.matches(requestUri)
    fun extractPathParams(requestUri: String) = parser.extractPathParams(requestUri)
}

/**
 * Every WebSocket request passes through a single instance of this class.
 * Session IDs are generated and tracked here, and path-parameters are cached for performance.
 */
@WebSocket
class JavalinWsRouter(private val wsEntries: List<WebSocketEntry>) {

    private val sessionIds = ConcurrentHashMap<Session, String>()
    private val sessionPathParams = ConcurrentHashMap<Session, Map<String, String>>()

    @OnWebSocketConnect
    fun webSocketConnect(session: Session) {
        findEntry(session)?.let { it.handler.connectHandler?.handle(wrap(session, it)) }
    }

    @OnWebSocketMessage
    fun webSocketMessage(session: Session, message: String) {
        findEntry(session)?.let { it.handler.messageHandler?.handle(wrap(session, it), message) }
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

    private fun wrap(session: Session, wsEntry: WebSocketEntry): WsSession {
        sessionIds.putIfAbsent(session, UUID.randomUUID().toString())
        sessionPathParams.putIfAbsent(session, wsEntry.extractPathParams(session.upgradeRequest.requestURI.path))
        return WsSession(sessionIds[session]!!, session, sessionPathParams[session]!!)
    }

    private fun destroy(session: Session) {
        sessionIds.remove(session)
        sessionPathParams.remove(session)
    }

}
