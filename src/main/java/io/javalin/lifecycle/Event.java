/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.lifecycle;

import io.javalin.Javalin;

public class Event {

    public enum Type {
        SERVER_STARTING,
        SERVER_STARTED,
        SERVER_STOPPING,
        SERVER_STOPPED
    }

    public Type eventType;
    public Javalin javalin;

    public Event(Type eventType) {
        this.eventType = eventType;
        this.javalin = null;
    }

    public Event(Type eventType, Javalin javalin) {
        this.eventType = eventType;
        this.javalin = javalin;
    }

}
