/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.websocket

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.*

@WebSocket
class WebSocketHandlerRoot(private val handlers: List<WebSocketHandler>) {

    @OnWebSocketConnect
    fun webSocketConnect(session: Session) {
        findHandler(session)?.onConnect(session)
    }

    @OnWebSocketMessage
    fun webSocketMessage(session: Session, message: String) {
        findHandler(session)?.onMessage(session, message)
    }

    @OnWebSocketClose
    fun webSocketClose(session: Session, statusCode: Int, reason: String?) {
        findHandler(session)?.onClose(session, statusCode, reason)
    }

    @OnWebSocketError
    fun webSocketError(session: Session, throwable: Throwable?) {
        findHandler(session)?.onError(session, throwable)
    }

    private fun findHandler(session: Session): WebSocketHandler? {
        return handlers.find { it.matches(session.upgradeRequest.requestURI.path) }
    }

}
