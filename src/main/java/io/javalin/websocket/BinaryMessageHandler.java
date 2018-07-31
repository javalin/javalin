/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket;

@FunctionalInterface
public interface BinaryMessageHandler {
    void handle(WsSession session, Byte[] msg, int offset, int length);
}
