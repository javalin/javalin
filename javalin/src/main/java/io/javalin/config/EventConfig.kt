/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.config;

import io.javalin.event.EventHandler
import io.javalin.event.EventManager;
import io.javalin.event.HandlerMetaInfo
import io.javalin.event.JavalinLifecycleEvent;
import io.javalin.event.WsHandlerMetaInfo
import java.util.function.Consumer

class EventConfig {

    @JvmField val eventManager = EventManager()

    fun serverStarting(eventHandler: EventHandler) = addLifecycleEvent(JavalinLifecycleEvent.SERVER_STARTING, eventHandler)
    fun serverStarted(eventHandler: EventHandler) = addLifecycleEvent(JavalinLifecycleEvent.SERVER_STARTED, eventHandler)
    fun serverStartFailed(eventHandler: EventHandler) = addLifecycleEvent(JavalinLifecycleEvent.SERVER_START_FAILED, eventHandler)
    fun serverStopFailed(eventHandler: EventHandler) = addLifecycleEvent(JavalinLifecycleEvent.SERVER_STOP_FAILED, eventHandler)
    fun serverStopping(eventHandler: EventHandler) = addLifecycleEvent(JavalinLifecycleEvent.SERVER_STOPPING, eventHandler)
    fun serverStopped(eventHandler: EventHandler) = addLifecycleEvent(JavalinLifecycleEvent.SERVER_STOPPED, eventHandler)

    fun handlerAdded(callback: Consumer<HandlerMetaInfo>) {
        this.eventManager.handlerAddedHandlers.add(callback);
    }

    fun wsHandlerAdded(callback: Consumer<WsHandlerMetaInfo>) {
        eventManager.wsHandlerAddedHandlers.add(callback);
    }

    private fun addLifecycleEvent(event: JavalinLifecycleEvent, eventHandler: EventHandler) {
        eventManager.lifecycleHandlers[event]!!.add(eventHandler)
    }

}
