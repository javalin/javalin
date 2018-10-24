/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.websocket.WsSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for logging the web socket lifecycle.
 *
 * @see WsSession
 */
@FunctionalInterface
public interface WsLifecycleLogger {
    /**
     * The method to be implemented
     * @param isClosed - true if the connection is closed, false if the connection has just been established
     * @param session - the corresponding session
     * @param statusCode - the closing statusCode. This parameter is only relevant if isClosed is true
     * @param reason - the optional reason for closure.
     */
    void handle(boolean isClosed, @NotNull WsSession session, int statusCode, @Nullable String reason);
}
