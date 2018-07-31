/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket;

import org.jetbrains.annotations.NotNull;

public class WsHandler {

    ConnectHandler connectHandler = null;
    MessageHandler messageHandler = null;
    BinaryMessageHandler binaryMessageHandler = null;
    CloseHandler closeHandler = null;
    ErrorHandler errorHandler = null;

    /**
     * Add a ConnectHandler to the WsHandler.
     * The handler is called when a WebSocket client connects.
     */
    public void onConnect(@NotNull ConnectHandler connectHandler) {
        this.connectHandler = connectHandler;
    }

    /**
     * Add a MessageHandler to the WsHandler.
     * The handler is called when a WebSocket client sends
     * a String message.
     */
    public void onMessage(@NotNull MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Add a {@link BinaryMessageHandler} to the WsHandler.
     * The handler is called when a WebSocket client sends
     * a binary message.
     */
    public void onMessage(@NotNull BinaryMessageHandler binaryMessageHandler) {
        this.binaryMessageHandler = binaryMessageHandler;
    }

    /**
     * Add a CloseHandler to the WsHandler.
     * The handler is called when a WebSocket client closes
     * the connection. The handler is not called in case of
     * network issues, only when the client actively closes the
     * connection (or times out).
     */
    public void onClose(@NotNull CloseHandler closeHandler) {
        this.closeHandler = closeHandler;
    }

    /**
     * Add a errorHandler to the WsHandler.
     * The handler is called when an error is detected.
     */
    public void onError(@NotNull ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

}
