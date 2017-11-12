/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty.websocket;

import java.util.Optional;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.jetbrains.annotations.NotNull;

@WebSocket
public class WebSocketHandler {

    private Optional<WsHandler> connectHandler;
    private Optional<WsHandler> messageHandler;
    private Optional<WsHandler> closeHandler;
    private Optional<WsHandler> errorHandler;

    public void onConnect(@NotNull WsHandler connectHandler) {
        this.connectHandler = Optional.of(connectHandler);
    }

    public void onMessage(@NotNull WsHandler messageHandler) {
        this.messageHandler = Optional.of(messageHandler);
    }

    public void onClose(@NotNull WsHandler closeHandler) {
        this.closeHandler = Optional.of(closeHandler);
    }

    public void onError(@NotNull WsHandler errorHandler) {
        this.errorHandler = Optional.of(errorHandler);
    }

    // Jetty annotations

    @OnWebSocketConnect
    public void _internalOnConnectProxy(Session session) {
        connectHandler.ifPresent(it -> {
            try {
                it.handle(new WsContext(session));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @OnWebSocketMessage
    public void _internalOnMessageProxy(Session session, String message) {
        messageHandler.ifPresent(it -> {
            try {
                it.handle(new WsContext(session, message));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @OnWebSocketClose
    public void _internalOnCloseProxy(Session session, int statusCode, String reason) {
        closeHandler.ifPresent(it -> {
            try {
                it.handle(new WsContext(session, statusCode, reason));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @OnWebSocketError
    public void _internalOnErrorProxy(Session session, Throwable throwable) {
        errorHandler.ifPresent(it -> {
            try {
                it.handle(new WsContext(session, throwable));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

}
