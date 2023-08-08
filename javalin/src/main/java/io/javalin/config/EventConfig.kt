/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.config;

import io.javalin.event.EventManager
import io.javalin.event.LifecycleEventListener
import io.javalin.event.HandlerMetaInfo
import io.javalin.event.JavalinLifecycleEvent.SERVER_STARTED
import io.javalin.event.JavalinLifecycleEvent.SERVER_STARTING
import io.javalin.event.JavalinLifecycleEvent.SERVER_START_FAILED
import io.javalin.event.JavalinLifecycleEvent.SERVER_STOPPED
import io.javalin.event.JavalinLifecycleEvent.SERVER_STOPPING
import io.javalin.event.JavalinLifecycleEvent.SERVER_STOP_FAILED
import io.javalin.event.WsHandlerMetaInfo
import java.util.function.Consumer

class EventConfig(private val cfg: JavalinConfig) {

    fun serverStarting(lifecycleEventListener: LifecycleEventListener) = eventManager.addLifecycleEvent(SERVER_STARTING, lifecycleEventListener)
    fun serverStarted(lifecycleEventListener: LifecycleEventListener) = eventManager.addLifecycleEvent(SERVER_STARTED, lifecycleEventListener)
    fun serverStartFailed(lifecycleEventListener: LifecycleEventListener) = eventManager.addLifecycleEvent(SERVER_START_FAILED, lifecycleEventListener)
    fun serverStopFailed(lifecycleEventListener: LifecycleEventListener) = eventManager.addLifecycleEvent(SERVER_STOP_FAILED, lifecycleEventListener)
    fun serverStopping(lifecycleEventListener: LifecycleEventListener) = eventManager.addLifecycleEvent(SERVER_STOPPING, lifecycleEventListener)
    fun serverStopped(lifecycleEventListener: LifecycleEventListener) = eventManager.addLifecycleEvent(SERVER_STOPPED, lifecycleEventListener)

    fun handlerAdded(callback: Consumer<HandlerMetaInfo>) {
        eventManager.handlerAddedHandlers.add(callback);
    }

    fun wsHandlerAdded(callback: Consumer<WsHandlerMetaInfo>) {
        eventManager.wsHandlerAddedHandlers.add(callback);
    }

    private val eventManager: EventManager
        get() = cfg.pvt.eventManager

}
