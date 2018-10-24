/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.websocket.WsSession;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for logging web socket messages.
 *
 * @see WsSession
 */
@FunctionalInterface
public interface WsMessageLogger {
    void handle(@NotNull WsSession session, @NotNull String message);
}
