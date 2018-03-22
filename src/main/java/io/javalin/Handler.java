/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

/**
 * Main interface for endpoint actions. A handler has a void return type,
 * so you have to use {@link Context#result} to return data to the client.
 *
 * @see Context
 * @see <a href="https://javalin.io/documentation#handlers">Handler in documentation</a>
 */
@FunctionalInterface
public interface Handler {
    void handle(Context ctx) throws Exception;
}
