/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

/**
 * A handler for use with {@link Javalin#error(int, ErrorHandler)}.
 * Is triggered by [{@link Context#status()}] codes at the end of the request lifecycle.
 *
 * @see Context
 * @see <a href="https://javalin.io/documentation#error-mapping">Error mapping in docs</a>
 */
@FunctionalInterface
public interface ErrorHandler {
    void handle(Context ctx);
}
