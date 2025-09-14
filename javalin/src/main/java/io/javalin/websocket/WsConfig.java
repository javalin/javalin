/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket;

import org.jetbrains.annotations.NotNull;

/**
 * Holds the different WebSocket handlers for a specific {@link WsHandlerEntry} or the WebSocket logger.
 */
public class WsConfig {

    WsConnectHandler wsConnectHandler = null;
    WsMessageHandler wsMessageHandler = null;
    WsBinaryMessageHandler wsBinaryMessageHandler = null;
    WsCloseHandler wsCloseHandler = null;
    WsErrorHandler wsErrorHandler = null;
    WsUpgradeLogger wsUpgradeLogger = null;

    /**
     * Add a WsConnectHandler to the WsHandler.
     * The handler is called when a WebSocket client connects.
     */
    public void onConnect(@NotNull WsConnectHandler wsConnectHandler) {
        this.wsConnectHandler = wsConnectHandler;
    }

    /**
     * Add a WsMessageHandler to the WsHandler.
     * The handler is called when a WebSocket client sends
     * a String message.
     */
    public void onMessage(@NotNull WsMessageHandler wsMessageHandler) {
        this.wsMessageHandler = wsMessageHandler;
    }

    /**
     * Add a {@link WsBinaryMessageHandler} to the WsHandler.
     * The handler is called when a WebSocket client sends
     * a binary message.
     */
    public void onBinaryMessage(@NotNull WsBinaryMessageHandler wsBinaryMessageHandler) {
        this.wsBinaryMessageHandler = wsBinaryMessageHandler;
    }

    /**
     * Add a WsCloseHandler to the WsHandler.
     * The handler is called when a WebSocket client closes
     * the connection. The handler is not called in case of
     * network issues, only when the client actively closes the
     * connection (or times out).
     */
    public void onClose(@NotNull WsCloseHandler wsCloseHandler) {
        this.wsCloseHandler = wsCloseHandler;
    }

    /**
     * Add a wsErrorHandler to the WsHandler.
     * The handler is called when an error is detected.
     */
    public void onError(@NotNull WsErrorHandler wsErrorHandler) {
        this.wsErrorHandler = wsErrorHandler;
    }

    /**
     * Add a WsUpgradeLogger to the WsHandler.
     * The handler is called when an HTTP request attempts to upgrade to WebSocket.
     * This is invoked before the upgrade process begins, whether the upgrade succeeds or fails.
     */
    public void onUpgrade(@NotNull WsUpgradeLogger wsUpgradeLogger) {
        this.wsUpgradeLogger = wsUpgradeLogger;
    }

    /**
     * Get the WebSocket upgrade logger, if configured
     */
    public WsUpgradeLogger getWsUpgradeLogger() {
        return wsUpgradeLogger;
    }

}
