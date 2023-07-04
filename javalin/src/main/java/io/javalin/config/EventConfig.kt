/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.config;

import io.javalin.event.LifecycleEventListener
import io.javalin.event.EventManager;
import io.javalin.event.HandlerMetaInfo
import io.javalin.event.JavalinLifecycleEvent;
import io.javalin.event.WsHandlerMetaInfo
import java.util.function.Consumer

class EventConfig {

    @JvmField val eventManager = EventManager()

    fun serverStarting(lifecycleEventListener: LifecycleEventListener) = addLifecycleEvent(JavalinLifecycleEvent.SERVER_STARTING, lifecycleEventListener)
    fun serverStarted(lifecycleEventListener: LifecycleEventListener) = addLifecycleEvent(JavalinLifecycleEvent.SERVER_STARTED, lifecycleEventListener)
    fun serverStartFailed(lifecycleEventListener: LifecycleEventListener) = addLifecycleEvent(JavalinLifecycleEvent.SERVER_START_FAILED, lifecycleEventListener)
    fun serverStopFailed(lifecycleEventListener: LifecycleEventListener) = addLifecycleEvent(JavalinLifecycleEvent.SERVER_STOP_FAILED, lifecycleEventListener)
    fun serverStopping(lifecycleEventListener: LifecycleEventListener) = addLifecycleEvent(JavalinLifecycleEvent.SERVER_STOPPING, lifecycleEventListener)
    fun serverStopped(lifecycleEventListener: LifecycleEventListener) = addLifecycleEvent(JavalinLifecycleEvent.SERVER_STOPPED, lifecycleEventListener)

    fun handlerAdded(callback: Consumer<HandlerMetaInfo>) {
        this.eventManager.handlerAddedHandlers.add(callback);
    }

    fun wsHandlerAdded(callback: Consumer<WsHandlerMetaInfo>) {
        eventManager.wsHandlerAddedHandlers.add(callback);
    }

    private fun addLifecycleEvent(event: JavalinLifecycleEvent, lifecycleEventListener: LifecycleEventListener) {
        eventManager.lifecycleHandlers[event]!!.add(lifecycleEventListener)
    }

}
