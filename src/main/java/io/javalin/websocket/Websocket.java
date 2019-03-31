package io.javalin.websocket;

import java.util.function.Consumer;

/**
 * Usage:
 *
 * ```java
 * public class FooWebsocket extends Websocket.Handler {
 *   .. implement only methods needed ..
 * }
 * ```
 *
 * Register websocket:
 * ```java
 *   app.ws("/my/websocket", Websocket.of(new FooWebsocket()))
 * ```
 */
public final class Websocket implements Consumer<WsHandler> {

    private final CloseHandler closeHandler;
    private final ConnectHandler connectHandler;
    private final MessageHandler messageHandler;
    private final BinaryMessageHandler binaryHandler;
    private final ErrorHandler errorHandler;

    private Websocket(CloseHandler closeHandler, ConnectHandler connectHandler, MessageHandler messageHandler, BinaryMessageHandler binaryHandler, ErrorHandler errorHandler) {
      this.closeHandler = closeHandler;
      this.connectHandler = connectHandler;
      this.messageHandler = messageHandler;
      this.binaryHandler = binaryHandler;
      this.errorHandler = errorHandler;
    }

    @Override
    public void accept(WsHandler t) {
        if (this.connectHandler != null) {
            t.onConnect(this.connectHandler);
        }

        if (this.closeHandler != null) {
            t.onClose(this.closeHandler);
        }

        if (this.errorHandler != null) {
            t.onError(this.errorHandler);
        }

        if (this.messageHandler != null) {
            t.onMessage(this.messageHandler);
        }

        if (this.binaryHandler != null) {
            t.onBinaryMessage(this.binaryHandler);
        }
    }

    public static Websocket of(Handler handlers) {
      return new Websocket(handlers, handlers, handlers, handlers, handlers);
    }

    public static Websocket of(ConnectHandler connect, CloseHandler close, MessageHandler msg, BinaryMessageHandler binaryMsg, ErrorHandler error) {
        return new Websocket(close, connect, msg, binaryMsg, error);
    }

    public static abstract class Handler implements CloseHandler, MessageHandler, ConnectHandler, ErrorHandler, BinaryMessageHandler {

        @Override
        public void handleConnect(WsConnectContext ctx) throws Exception {}

        @Override
        public void handleClose(WsCloseContext ctx) throws Exception {}

        @Override
        public void handleMessage(WsMessageContext ctx) throws Exception {}

        @Override
        public void handleBinaryMessage(WsBinaryMessageContext ctx) throws Exception {}

        @Override
        public void handleError(WsErrorContext ctx) throws Exception {}
    }
}
