/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket;

import io.javalin.http.Context;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for logging WebSocket upgrade requests.
 *
 * @see Context
 * @see <a href="https://javalin.io/documentation#request-loggers">RequestLogger in documentation</a>
 */
@FunctionalInterface
public interface WsUpgradeLogger {
    /**
     * Handles a WebSocket upgrade request
     *
     * @param ctx             the current request context during upgrade
     * @param executionTimeMs the requests' execution time in milliseconds
     * @throws Exception any exception while logging information about the request
     */
    void handle(@NotNull Context ctx, @NotNull Float executionTimeMs) throws Exception;
}