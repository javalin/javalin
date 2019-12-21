/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface WsBinaryMessageHandler {
    void handleBinaryMessage(@NotNull WsBinaryMessageContext ctx) throws Exception;
}
