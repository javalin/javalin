/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for logging requests.
 *
 * @see Context
 * @see <a href="https://javalin.io/documentation#request-loggers">RequestLogger in documentation</a>
 */
@FunctionalInterface
public interface RequestLogger {
    void handle(@NotNull Context ctx, @NotNull Float executionTimeMs) throws Exception;
}
