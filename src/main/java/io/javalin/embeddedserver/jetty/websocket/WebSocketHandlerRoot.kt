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
    fun webSocketConnect(session: Session) = findWebSocketHandlers(session).forEach { it.onConnect(session) }

    @OnWebSocketMessage
    fun webSocketMessage(session: Session, message: String) = findWebSocketHandlers(session).forEach { it.onMessage(session, message) }

    @OnWebSocketClose
    fun webSocketClose(session: Session, statusCode: Int, reason: String?) = findWebSocketHandlers(session).forEach { it.onClose(session, statusCode, reason) }

    @OnWebSocketError
    fun webSocketError(session: Session, throwable: Throwable?) = findWebSocketHandlers(session).forEach { it.onError(session, throwable) }

    private fun findWebSocketHandlers(session: Session) = handlers.filter { it.match(session.upgradeRequest.requestURI.path) }

}