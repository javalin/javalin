/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket;

import org.jetbrains.annotations.NotNull;

/**
 * A handler for use with {@link io.javalin.Javalin#wsException(Class, WsExceptionHandler)}.
 * Is triggered when an exception is thrown by a {@link WsHandler}.
 *
 * @see WsContext
 */
@FunctionalInterface
public interface WsExceptionHandler<T extends Exception> {
    void handle(@NotNull T exception, @NotNull WsContext ctx);
}
