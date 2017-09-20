/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty.websocket;

import java.io.IOException;
import java.util.Optional;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import io.javalin.embeddedserver.jetty.websocket.interfaces.CloseHandler;
import io.javalin.embeddedserver.jetty.websocket.interfaces.ConnectHandler;
import io.javalin.embeddedserver.jetty.websocket.interfaces.ErrorHandler;
import io.javalin.embeddedserver.jetty.websocket.interfaces.MessageHandler;

public class WebSocketHandler {
    private Optional<ConnectHandler> connectHandler;
    private Optional<MessageHandler> messageHandler;
    private Optional<CloseHandler> closeHandler;
    private Optional<ErrorHandler> errorHandler;

    private WebSocketAdapter webSocketAdapter = new WebSocketAdapter() {

        public void onWebSocketConnect(Session session) {
            super.onWebSocketConnect(session);
            connectHandler.ifPresent(it -> it.handle(session));
        }

        public void onWebSocketText(String message) {
            messageHandler.ifPresent(it -> it.handle(message));
        }

        public void onWebSocketClose(int statusCode, String reason) {
            closeHandler.ifPresent(it -> it.handle(statusCode, reason));
            super.onWebSocketClose(statusCode, reason);
        }

        public void onWebSocketError(Throwable throwable) {
            errorHandler.ifPresent(it -> it.handle(throwable));
        }
    };

    public WebSocketAdapter getWebSocketAdapter() {
        return webSocketAdapter;
    }

    public void onConnect(ConnectHandler connectHandler) {
        this.connectHandler = Optional.of(connectHandler);
    }

    public void onMessage(MessageHandler messageHandler) {
        this.messageHandler = Optional.of(messageHandler);
    }

    public void onClose(CloseHandler closeHandler) {
        this.closeHandler = Optional.of(closeHandler);
    }

    public void onError(ErrorHandler errorHandler) {
        this.errorHandler = Optional.of(errorHandler);
    }

    public void send(String msg) {
        try {
            this.webSocketAdapter.getRemote().sendString(msg);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Session session() {
        return this.webSocketAdapter.getSession();
    }

    public RemoteEndpoint remote() {
        return this.webSocketAdapter.getRemote();
    }

    public boolean isConnected() {
        return this.webSocketAdapter.isConnected();
    }

    public boolean isNotConnected() {
        return this.webSocketAdapter.isNotConnected();
    }

}
