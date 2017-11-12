/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty.websocket;

import io.javalin.embeddedserver.jetty.websocket.WsContext;

@FunctionalInterface
public interface WsHandler {
    void handle(WsContext wsCtx) throws Exception;
}
