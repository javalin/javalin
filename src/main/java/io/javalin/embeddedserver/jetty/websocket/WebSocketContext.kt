/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty.websocket

import io.javalin.embeddedserver.jetty.websocket.interfaces.CloseHandler
import io.javalin.embeddedserver.jetty.websocket.interfaces.ConnectHandler
import io.javalin.embeddedserver.jetty.websocket.interfaces.ErrorHandler
import io.javalin.embeddedserver.jetty.websocket.interfaces.MessageHandler
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter

class WebSocketContext {

    private var connectHandler = ConnectHandler { _ -> }
    private var messageHandler = MessageHandler { _ -> }
    private var closeHandler = CloseHandler { _, _ -> }
    private var errorHandler = ErrorHandler { _ -> }

    fun onConnect(connectHandler: ConnectHandler) {
        this.connectHandler = connectHandler
    }

    fun onMessage(messageHandler: MessageHandler) {
        this.messageHandler = messageHandler
    }

    fun onClose(closeHandler: CloseHandler) {
        this.closeHandler = closeHandler
    }

    fun onError(errorHandler: ErrorHandler) {
        this.errorHandler = errorHandler
    }

    val webSocketAdapter = object : WebSocketAdapter() {

        override fun onWebSocketConnect(session: Session) {
            super.onWebSocketConnect(session) // set session/remote
            connectHandler.handle(session)
        }

        override fun onWebSocketText(message: String?) {
            messageHandler.handle(message)
        }

        override fun onWebSocketClose(statusCode: Int, reason: String?) {
            closeHandler.handle(statusCode, reason)
            super.onWebSocketClose(statusCode, reason) // set session/remote to null
        }

        override fun onWebSocketError(throwable: Throwable?) {
            errorHandler.handle(throwable)
        }

    }

    fun send(msg: String) = webSocketAdapter.remote.sendString(msg)

    fun session() = webSocketAdapter.session;
    fun remote() = webSocketAdapter.remote;
    fun isConnected() = webSocketAdapter.isConnected
    fun isNotConnected() = webSocketAdapter.isNotConnected
}
