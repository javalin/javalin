package io.javalin.websocket;

import io.javalin.websocket.handler.CloseHandler;
import io.javalin.websocket.handler.ConnectHandler;
import io.javalin.websocket.handler.ErrorHandler;
import io.javalin.websocket.handler.MessageHandler;
import org.jetbrains.annotations.NotNull;

public class WebSocketHandler {

    ConnectHandler connectHandler = null;
    MessageHandler messageHandler = null;
    CloseHandler closeHandler = null;
    ErrorHandler errorHandler = null;

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

}
