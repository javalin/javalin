/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty.websocket.interfaces;

import io.javalin.embeddedserver.jetty.websocket.WsSession;

@FunctionalInterface
public interface CloseHandler {
    void handle(WsSession session, int statusCode, String reason) throws Exception;
}
