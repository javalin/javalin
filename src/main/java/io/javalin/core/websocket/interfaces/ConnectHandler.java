/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.websocket.interfaces;

import io.javalin.core.websocket.WsSession;

@FunctionalInterface
public interface ConnectHandler {
    void handle(WsSession session) throws Exception;
}
