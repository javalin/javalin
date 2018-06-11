/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin

import java.util.*

class EventManager {
    val listenerMap = JavalinEvent.values().associate { it to ArrayList<EventListener>() }
    fun fireEvent(javalinEvent: JavalinEvent) = listenerMap[javalinEvent]!!.forEach { listener -> listener.handleEvent() }
}

enum class JavalinEvent {
    SERVER_STARTING,
    SERVER_STARTED,
    SERVER_START_FAILED,
    SERVER_STOPPING,
    SERVER_STOPPED
}
