/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty.websocket;

/**
 * WebSocketConfig is a functional interface which is used
 * to configure a WebSocketHandler. The recommended shortname
 * for the WebSocketHandler is "ws":
 * <pre>
 * {@code
 * app.ws("/my-websocket-path", ws -> {
 *     ws.onConnect(session -> ...);
 *     ws.onMessage((session, message) -> ...);
 *     ws.onClose((session, statusCode, reason) -> ...);
 *     ws.onError((session, throwable) -> ...);
 * });
 * }
 * </pre>
 *
 * @see WebSocketHandler
 * @see io.javalin.Javalin#ws(String, WebSocketConfig)
 */
@FunctionalInterface
public interface WebSocketConfig {
    void configure(WebSocketHandler webSocketHandler);
}
