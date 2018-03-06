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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.jetbrains.annotations.NotNull;

@WebSocket
public class WebSocketHandler {

    private final ConcurrentMap<Session, String> sessions = new ConcurrentHashMap<>();

    private ConnectHandler connectHandler = null;
    private MessageHandler messageHandler = null;
    private CloseHandler closeHandler = null;
    private ErrorHandler errorHandler = null;

    /**
     * Add a ConnectHandler to the WebSocketConfig.
     * The handler is called when a WebSocket client connects.
     */
    public void onConnect(@NotNull ConnectHandler connectHandler) {
        this.connectHandler = connectHandler;
    }

    /**
     * Add a MessageHandler to the WebSocketConfig.
     * The handler is called when a WebSocket client sends
     * a String message.
     */
    public void onMessage(@NotNull MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Add a CloseHandler to the WebSocketConfig.
     * The handler is called when a WebSocket client closes
     * the connection. The handler is not called in case of
     * network issues, only when the client actively closes the
     * connection (or times out).
     */
    public void onClose(@NotNull CloseHandler closeHandler) {
        this.closeHandler = closeHandler;
    }

    /**
     * Add a errorHandler to the WebSocketConfig.
     * The handler is called when an error is detected.
     */
    public void onError(@NotNull ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    // Jetty annotations, nothing to see here.

    @OnWebSocketConnect
    public void _internalOnConnectProxy(Session session) throws Exception {
        WsSession wsSession = registerAndWrapSession(session);
        if (connectHandler != null) {
            connectHandler.handle(wsSession);
        }
    }

    @OnWebSocketMessage
    public void _internalOnMessageProxy(Session session, String message) throws Exception {
        WsSession wsSession = registerAndWrapSession(session);
        if (messageHandler != null) {
            messageHandler.handle(wsSession, message);
        }
    }

    @OnWebSocketClose
    public void _internalOnCloseProxy(Session session, int statusCode, String reason) throws Exception {
        WsSession wsSession = registerAndWrapSession(session);
        if (closeHandler != null) {
            closeHandler.handle(wsSession, statusCode, reason);
        }
        sessions.remove(session);
    }

    @OnWebSocketError
    public void _internalOnErrorProxy(Session session, Throwable throwable) throws Exception {
        WsSession wsSession = registerAndWrapSession(session);
        if (errorHandler != null) {
            errorHandler.handle(wsSession, throwable);
        }
    }

    private WsSession registerAndWrapSession(Session session) {
        sessions.putIfAbsent(session, UUID.randomUUID().toString());
        return new WsSession(sessions.get(session), session);
    }

}
