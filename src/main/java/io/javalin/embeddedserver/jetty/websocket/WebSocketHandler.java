/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty.websocket;

import io.javalin.embeddedserver.jetty.websocket.interfaces.CloseHandler;
import io.javalin.embeddedserver.jetty.websocket.interfaces.ConnectHandler;
import io.javalin.embeddedserver.jetty.websocket.interfaces.ErrorHandler;
import io.javalin.embeddedserver.jetty.websocket.interfaces.MessageHandler;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.jetbrains.annotations.NotNull;

@WebSocket
public class WebSocketHandler {

    private ConnectHandler connectHandler = null;
    private MessageHandler messageHandler = null;
    private CloseHandler closeHandler = null;
    private ErrorHandler errorHandler = null;

    public void onConnect(@NotNull ConnectHandler connectHandler) {
        this.connectHandler = connectHandler;
    }

    public void onMessage(@NotNull MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void onClose(@NotNull CloseHandler closeHandler) {
        this.closeHandler = closeHandler;
    }

    public void onError(@NotNull ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    // Jetty annotations

    @OnWebSocketConnect
    public void _internalOnConnectProxy(Session session) throws Exception {
        if (connectHandler != null) {
            connectHandler.handle(new WsSession(session));
        }
    }

    @OnWebSocketMessage
    public void _internalOnMessageProxy(Session session, String message) throws Exception {
        if (messageHandler != null) {
            messageHandler.handle(new WsSession(session), message);
        }
    }

    @OnWebSocketClose
    public void _internalOnCloseProxy(Session session, int statusCode, String reason) throws Exception {
        if (closeHandler != null) {
            closeHandler.handle(new WsSession(session), statusCode, reason);
        }
    }

    @OnWebSocketError
    public void _internalOnErrorProxy(Session session, Throwable throwable) throws Exception {
        if (errorHandler != null) {
            errorHandler.handle(new WsSession(session), throwable);
        }
    }

}
