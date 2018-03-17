/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.embeddedserver.jetty.websocket.WebSocketHandler
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.*

@WebSocket
class JavalinWebSocketHandler(private val handlers: List<WebSocketHandler>) {

    @OnWebSocketConnect
    fun webSocketConnect(session: Session) {
        println("Connected: " + session.upgradeRequest.requestURI.path)
        findWebSocketHandlers(session.upgradeRequest.requestURI.path).forEach { handler ->  handler._internalOnConnectProxy(session)}
    }

    @OnWebSocketMessage
    fun webSocketMessage(session: Session, message: String) {
        println("Message: $message")
        findWebSocketHandlers(session.upgradeRequest.requestURI.path).forEach { handler ->  handler._internalOnMessageProxy(session, message)}
    }

    @OnWebSocketClose
    fun webSocketClose(session: Session, statusCode: Int, reason: String) {
        println("Close:" + session.upgradeRequest.requestURI.path)
        findWebSocketHandlers(session.upgradeRequest.requestURI.path).forEach { handler ->  handler._internalOnCloseProxy(session, statusCode, reason)}
    }

    @OnWebSocketError
    fun webSocketError(session: Session, throwable: Throwable) {
        println("Error")
        findWebSocketHandlers(session.upgradeRequest.requestURI.path).forEach { handler ->  handler._internalOnErrorProxy(session, throwable)}
    }

    private fun findWebSocketHandlers(requestUri: String): List<WebSocketHandler> {
        return handlers.filter { handler ->  handler.match(requestUri)}
    }

}