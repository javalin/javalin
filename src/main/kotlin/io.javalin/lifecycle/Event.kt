/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.lifecycle

import io.javalin.Javalin

class Event {

    enum class Type {
        SERVER_STARTING,
        SERVER_STARTED,
        SERVER_STOPPING,
        SERVER_STOPPED
    }

    var eventType: Type
    var javalin: Javalin? = null

    constructor(eventType: Type) {
        this.eventType = eventType
    }

    constructor(eventType: Type, javalin: Javalin) {
        this.eventType = eventType
        this.javalin = javalin
    }

}
