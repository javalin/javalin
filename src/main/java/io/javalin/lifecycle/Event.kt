/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.lifecycle

import io.javalin.Javalin

data class Event(var eventType: Type, var javalin: Javalin? = null) {
    enum class Type {
        SERVER_STARTING,
        SERVER_STARTED,
        SERVER_START_FAILED,
        SERVER_STOPPING,
        SERVER_STOPPED
    }
}
