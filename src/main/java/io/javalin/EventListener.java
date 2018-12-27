/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

/**
 * A listener for use with {@link Javalin#event}.
 * Is triggered by different events in the app lifecycle.
 */
@FunctionalInterface
public interface EventListener {
    void handleEvent() throws Exception;
}
