/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for logging the registration of handlers.
 */
@FunctionalInterface
public interface HandlerRegistrationLogger {

    /**
     * Executed when a new handler is added.
     *
     * Implement this method to provide custom logging for each new added handler.
     *
     * @param httpMethod The {@linkplain HandlerType} of the added handler.
     * @param path The relative path of the added handler.
     */
    void handle(@NotNull HandlerType httpMethod, @NotNull String path);
}
