/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.event

/**
 * Main interface for Lifecycle Event Handlers.
 * A Runnable does not suffice because the event handler may throw a checked exception.
 *
 * @see <a href="https://javalin.io/documentation#lifecycle-events">Lifecycle Events in documentation</a>
 */
fun interface LifecycleEventListener {
    @Throws(Exception::class) fun handleEvent()
}
