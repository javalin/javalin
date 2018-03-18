/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty.websocket

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.*

@WebSocket
class WebSocketHandlerRoot(private val handlers: List<WebSocketHandler>) {

    @OnWebSocketConnect
    fun webSocketConnect(session: Session) = findHandlers(session).forEach { it.onConnect(session) }

    @OnWebSocketMessage
    fun webSocketMessage(session: Session, message: String) = findHandlers(session).forEach { it.onMessage(session, message) }

    @OnWebSocketClose
    fun webSocketClose(session: Session, statusCode: Int, reason: String?) = findHandlers(session).forEach { it.onClose(session, statusCode, reason) }

    @OnWebSocketError
    fun webSocketError(session: Session, throwable: Throwable?) = findHandlers(session).forEach { it.onError(session, throwable) }

    private fun findHandlers(session: Session) = handlers.filter { it.matches(session.upgradeRequest.requestURI.path) }

}
