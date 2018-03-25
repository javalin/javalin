/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty.websocket;

import io.javalin.core.PathParser;
import io.javalin.embeddedserver.jetty.websocket.interfaces.CloseHandler;
import io.javalin.embeddedserver.jetty.websocket.interfaces.ConnectHandler;
import io.javalin.embeddedserver.jetty.websocket.interfaces.ErrorHandler;
import io.javalin.embeddedserver.jetty.websocket.interfaces.MessageHandler;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.jetbrains.annotations.NotNull;

@WebSocket
public class WebSocketHandler {

    public WebSocketHandler(@NotNull String contextPath, @NotNull String path) {
        pathParser = new PathParser(contextPath + path);
    }

    private final PathParser pathParser;
    private final ConcurrentMap<Session, Map<String, String>> sessionParams = new ConcurrentHashMap<>();
    private final ConcurrentMap<Session, String> sessionIds = new ConcurrentHashMap<>();

    private ConnectHandler connectHandler = null;
    private MessageHandler messageHandler = null;
    private CloseHandler closeHandler = null;
    private ErrorHandler errorHandler = null;

    /**
     * Add a ConnectHandler to the WebSocketHandler.
     * The handler is called when a WebSocket client connects.
     */
    public void onConnect(@NotNull ConnectHandler connectHandler) {
        this.connectHandler = connectHandler;
    }

    /**
     * Add a MessageHandler to the WebSocketHandler.
     * The handler is called when a WebSocket client sends
     * a String message.
     */
    public void onMessage(@NotNull MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Add a CloseHandler to the WebSocketHandler.
     * The handler is called when a WebSocket client closes
     * the connection. The handler is not called in case of
     * network issues, only when the client actively closes the
     * connection (or times out).
     */
    public void onClose(@NotNull CloseHandler closeHandler) {
        this.closeHandler = closeHandler;
    }

    /**
     * Add a errorHandler to the WebSocketHandler.
     * The handler is called when an error is detected.
     */
    public void onError(@NotNull ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    void onConnect(Session session) throws Exception {
        WsSession wsSession = registerAndWrapSession(session);
        if (connectHandler != null) {
            connectHandler.handle(wsSession);
        }
    }

    void onMessage(Session session, String message) throws Exception {
        WsSession wsSession = registerAndWrapSession(session);
        if (messageHandler != null) {
            messageHandler.handle(wsSession, message);
        }
    }

    void onClose(Session session, int statusCode, String reason) throws Exception {
        WsSession wsSession = registerAndWrapSession(session);
        if (closeHandler != null) {
            closeHandler.handle(wsSession, statusCode, reason);
        }
        clearSessionCache(session);
    }

    void onError(Session session, Throwable throwable) throws Exception {
        WsSession wsSession = registerAndWrapSession(session);
        if (errorHandler != null) {
            errorHandler.handle(wsSession, throwable);
        }
    }

    public boolean matches(String requestUri) {
        return pathParser.matches(requestUri);
    }

    private void clearSessionCache(Session session){
        sessionIds.remove(session);
        sessionParams.remove(session);
    }

    private WsSession registerAndWrapSession(Session session) {
        sessionIds.putIfAbsent(session, UUID.randomUUID().toString());
        sessionParams.putIfAbsent(session, pathParser.extractParams(session.getUpgradeRequest().getRequestURI().getPath()));
        return new WsSession(sessionIds.get(session), session, sessionParams.get(session));
    }
}
