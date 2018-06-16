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

data class WebSocketEntry(val path: String, val wsHandler: WebSocketHandler)

@WebSocket
class JavalinWsRouter(private val wsEntries: List<WebSocketEntry>) {

    private val sessionPathParams = ConcurrentHashMap<Session, Map<String, String>>()
    private val sessionIds = ConcurrentHashMap<Session, String>()

    @OnWebSocketConnect
    fun webSocketConnect(session: Session) {
        findEntry(session)?.let { it.wsHandler.connectHandler?.handle(wrap(session, it)) }
    }

    @OnWebSocketMessage
    fun webSocketMessage(session: Session, message: String) {
        findEntry(session)?.let { it.wsHandler.messageHandler?.handle(wrap(session, it), message) }
    }

    @OnWebSocketError
    fun webSocketError(session: Session, throwable: Throwable?) {
        findEntry(session)?.let { it.wsHandler.errorHandler?.handle(wrap(session, it), throwable) }
    }

    @OnWebSocketClose
    fun webSocketClose(session: Session, statusCode: Int, reason: String?) {
        findEntry(session)?.let { it.wsHandler.closeHandler?.handle(wrap(session, it), statusCode, reason) }
        destroy(session)
    }

    private fun findEntry(session: Session) = findEntry(session.upgradeRequest)

    fun findEntry(req: UpgradeRequest) = wsEntries.find {
        PathParser(it.path).matches(req.requestURI.path)
    }

    private fun pathParams(session: Session, wsEntry: WebSocketEntry) =
        PathParser(wsEntry.path).extractPathParams(session.upgradeRequest.requestURI.path)

    private fun wrap(session: Session, wsEntry: WebSocketEntry): WsSession {
        sessionIds.putIfAbsent(session, UUID.randomUUID().toString())
        sessionPathParams.putIfAbsent(session, pathParams(session, wsEntry))
        return WsSession(sessionIds[session]!!, session, sessionPathParams[session]!!)
    }

    private fun destroy(session: Session) {
        sessionIds.remove(session)
        sessionPathParams.remove(session)
    }

}
