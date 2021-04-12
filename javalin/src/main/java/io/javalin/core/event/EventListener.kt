/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.core.event

import java.util.function.Consumer

class EventListener(private val eventManager: EventManager) {

    fun serverStarting(eventHandler: EventHandler) = addLifecycleEvent(JavalinEvent.SERVER_STARTING, eventHandler)
    fun serverStarted(eventHandler: EventHandler) = addLifecycleEvent(JavalinEvent.SERVER_STARTED, eventHandler)
    fun serverStartFailed(eventHandler: EventHandler) = addLifecycleEvent(JavalinEvent.SERVER_START_FAILED, eventHandler)
    fun serverStopping(eventHandler: EventHandler) = addLifecycleEvent(JavalinEvent.SERVER_STOPPING, eventHandler)
    fun serverStopped(eventHandler: EventHandler) = addLifecycleEvent(JavalinEvent.SERVER_STOPPED, eventHandler)
    fun handlerAdded(callback: Consumer<HandlerMetaInfo>) = eventManager.handlerAddedHandlers.add(callback)
    fun wsHandlerAdded(callback: Consumer<WsHandlerMetaInfo>) = eventManager.wsHandlerAddedHandlers.add(callback)

    private fun addLifecycleEvent(event: JavalinEvent, eventHandler: EventHandler) {
        eventManager.lifecycleHandlers[event]!!.add(eventHandler)
    }
}
