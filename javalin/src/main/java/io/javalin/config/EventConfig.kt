/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.config;

import io.javalin.event.EventManager
import io.javalin.event.HandlerMetaInfo
import io.javalin.event.JavalinLifecycleEvent.SERVER_STARTED
import io.javalin.event.JavalinLifecycleEvent.SERVER_STARTING
import io.javalin.event.JavalinLifecycleEvent.SERVER_START_FAILED
import io.javalin.event.JavalinLifecycleEvent.SERVER_STOPPED
import io.javalin.event.JavalinLifecycleEvent.SERVER_STOPPING
import io.javalin.event.JavalinLifecycleEvent.SERVER_STOP_FAILED
import io.javalin.event.LifecycleEventListener
import io.javalin.event.WsHandlerMetaInfo
import io.javalin.http.Handler
import java.util.function.Consumer

/**
 * Configures the events' listener.
 *
 * @param cfg the parent Javalin Configuration
 * @see [JavalinConfig.events]
 */
class EventConfig(private val cfg: JavalinConfig) {

    /** Adds a callback to react on the Server Starting event. */
    fun serverStarting(lifecycleEventListener: LifecycleEventListener) = eventManager.addLifecycleEvent(SERVER_STARTING, lifecycleEventListener)
    /** Adds a callback to react on the Server Started event. */
    fun serverStarted(lifecycleEventListener: LifecycleEventListener) = eventManager.addLifecycleEvent(SERVER_STARTED, lifecycleEventListener)
    /** Adds a callback to react on the Server Start Failed event. */
    fun serverStartFailed(lifecycleEventListener: LifecycleEventListener) = eventManager.addLifecycleEvent(SERVER_START_FAILED, lifecycleEventListener)
    /** Adds a callback to react on the Server Stop Failed event. */
    fun serverStopFailed(lifecycleEventListener: LifecycleEventListener) = eventManager.addLifecycleEvent(SERVER_STOP_FAILED, lifecycleEventListener)
    /** Adds a callback to react on the Server Stopping event. */
    fun serverStopping(lifecycleEventListener: LifecycleEventListener) = eventManager.addLifecycleEvent(SERVER_STOPPING, lifecycleEventListener)
    /** Adds a callback to react on the Server Stopped event. */
    fun serverStopped(lifecycleEventListener: LifecycleEventListener) = eventManager.addLifecycleEvent(SERVER_STOPPED, lifecycleEventListener)

    /** Adds a callback to react when a [Handler] is added. */
    fun handlerAdded(callback: Consumer<HandlerMetaInfo>) {
        eventManager.handlerAddedHandlers.add(callback);
    }

    /** Adds a callback to react when a websocket Handler is added. */
    fun wsHandlerAdded(callback: Consumer<WsHandlerMetaInfo>) {
        eventManager.wsHandlerAddedHandlers.add(callback);
    }

    /**
     * Registers a handler for a user-defined event type.
     * @param eventClass the class of the event
     * @param handler the handler to be called when the event is emitted
     */
    fun <T : Any> on(eventClass: Class<T>, handler: Consumer<T>) {
        eventManager.on(eventClass, handler)
    }

    private val eventManager: EventManager
        get() = cfg.pvt.eventManager

}
