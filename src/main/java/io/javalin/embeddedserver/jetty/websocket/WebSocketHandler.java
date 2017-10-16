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

import io.javalin.embeddedserver.jetty.websocket.interfaces.CloseHandler;
import io.javalin.embeddedserver.jetty.websocket.interfaces.ConnectHandler;
import io.javalin.embeddedserver.jetty.websocket.interfaces.ErrorHandler;
import io.javalin.embeddedserver.jetty.websocket.interfaces.MessageHandler;

@WebSocket
public class WebSocketHandler {

    private Optional<ConnectHandler> connectHandler;
    private Optional<MessageHandler> messageHandler;
    private Optional<CloseHandler> closeHandler;
    private Optional<ErrorHandler> errorHandler;

    public void onConnect(@NotNull ConnectHandler connectHandler) {
        this.connectHandler = Optional.of(connectHandler);
    }

    public void onMessage(@NotNull MessageHandler messageHandler) {
        this.messageHandler = Optional.of(messageHandler);
    }

    public void onClose(@NotNull CloseHandler closeHandler) {
        this.closeHandler = Optional.of(closeHandler);
    }

    public void onError(@NotNull ErrorHandler errorHandler) {
        this.errorHandler = Optional.of(errorHandler);
    }

    // Jetty annotations

    @OnWebSocketConnect
    public void _internalOnConnectProxy(Session session) {
        connectHandler.ifPresent(it -> {
            try {
                it.handle(session);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @OnWebSocketMessage
    public void _internalOnMessageProxy(Session session, String message) {
        messageHandler.ifPresent(it -> {
            try {
                it.handle(session, message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @OnWebSocketClose
    public void _internalOnCloseProxy(Session session, int statusCode, String reason) {
        closeHandler.ifPresent(it -> {
            try {
                it.handle(session, statusCode, reason);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @OnWebSocketError
    public void _internalOnErrorProxy(Session session, Throwable throwable) {
        errorHandler.ifPresent(it -> {
            try {
                it.handle(session, throwable);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

}
