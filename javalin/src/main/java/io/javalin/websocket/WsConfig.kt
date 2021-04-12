/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.websocket

/**
 * Holds the different WebSocket handlers for a specific [WsEntry] or the WebSocket logger.
 */
class WsConfig {

    var wsConnectHandler: WsConnectHandler? = null
    var wsMessageHandler: WsMessageHandler? = null
    var wsBinaryMessageHandler: WsBinaryMessageHandler? = null
    var wsCloseHandler: WsCloseHandler? = null
    var wsErrorHandler: WsErrorHandler? = null

    /**
     * Add a WsConnectHandler to the WsHandler.
     * The handler is called when a WebSocket client connects.
     */
    fun onConnect(wsConnectHandler: WsConnectHandler) {
        this.wsConnectHandler = wsConnectHandler
    }

    /**
     * Add a WsMessageHandler to the WsHandler.
     * The handler is called when a WebSocket client sends
     * a String message.
     */
    fun onMessage(wsMessageHandler: WsMessageHandler) {
        this.wsMessageHandler = wsMessageHandler
    }

    /**
     * Add a [WsBinaryMessageHandler] to the WsHandler.
     * The handler is called when a WebSocket client sends
     * a binary message.
     */
    fun onBinaryMessage(wsBinaryMessageHandler: WsBinaryMessageHandler) {
        this.wsBinaryMessageHandler = wsBinaryMessageHandler
    }

    /**
     * Add a WsCloseHandler to the WsHandler.
     * The handler is called when a WebSocket client closes
     * the connection. The handler is not called in case of
     * network issues, only when the client actively closes the
     * connection (or times out).
     */
    fun onClose(wsCloseHandler: WsCloseHandler) {
        this.wsCloseHandler = wsCloseHandler
    }

    /**
     * Add a wsErrorHandler to the WsHandler.
     * The handler is called when an error is detected.
     */
    fun onError(wsErrorHandler: WsErrorHandler) {
        this.wsErrorHandler = wsErrorHandler
    }

}
