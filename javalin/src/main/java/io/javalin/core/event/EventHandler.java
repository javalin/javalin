/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.event;

/**
 * Main interface for Lifecycle Event Handlers. A Runnable does not suffice because
 * the an event handler may throw a checked exception.
 *
 * @see <a href="https://javalin.io/documentation#lifecycle-events">Lifecycle Events in documentation</a>
 */
@FunctionalInterface
public interface EventHandler {
    void handleEvent() throws Exception;
}
