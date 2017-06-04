/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.lifecycle

import io.javalin.Javalin
import java.util.*

class EventManager {

    private val listenerMap: Map<Event.Type, LinkedList<EventListener>> = Event.Type.values().map { it to LinkedList<EventListener>() }.toMap()

    fun addEventListener(type: Event.Type, listener: EventListener) {
        listenerMap[type]!!.add(listener)
    }

    fun fireEvent(type: Event.Type, javalin: Javalin) {
        listenerMap[type]!!.forEach { listener -> listener.handleEvent(Event(type, javalin)) }
    }

}
