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

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@WebSocket
public class WebSocketHandler {

    private final ConcurrentMap<Session, String> sessions = new ConcurrentHashMap<>();

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

    public Collection<WsSession> getSessions() {
        return sessions.keySet()
                .stream()
                .filter(Session::isOpen)
                .map(this::createWsSession)
                .collect(Collectors.toList());
    }

    // Jetty annotations

    @OnWebSocketConnect
    public void _internalOnConnectProxy(Session session) throws Exception {
        String sessionId = registerSession(session);
        if (connectHandler != null) {
            connectHandler.handle(createWsSession(session));
        }
    }

    @OnWebSocketMessage
    public void _internalOnMessageProxy(Session session, String message) throws Exception {
        String sessionId = registerSession(session);
        if (messageHandler != null) {
            messageHandler.handle(createWsSession(session), message);
        }
    }

    @OnWebSocketClose
    public void _internalOnCloseProxy(Session session, int statusCode, String reason) throws Exception {
        String sessionId = registerSession(session);
        if (closeHandler != null) {
            closeHandler.handle(createWsSession(session), statusCode, reason);
        }

        sessions.remove(session);
    }

    @OnWebSocketError
    public void _internalOnErrorProxy(Session session, Throwable throwable) throws Exception {
        String sessionId = registerSession(session);
        if (errorHandler != null) {
            errorHandler.handle(createWsSession(session), throwable);
        }
    }

    private String registerSession(Session session) {
        return Objects.requireNonNull(sessions.computeIfAbsent(session, (s) -> nextSessionId()));
    }

    private void unregisterSession(Session session) {
        sessions.remove(Objects.requireNonNull(session));
    }

    private WsSession createWsSession(Session session) {
        return new WsSession(getSessionId(session), session);
    }

    private String getSessionId(Session session) {
        return sessions.get(session);
    }

    private String nextSessionId() {
        return UUID.randomUUID().toString();
    }
}
