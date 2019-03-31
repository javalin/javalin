/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.security.Role
import java.util.*

class EventManager {
    val eventAttacher = EventAttacher(this)
    val callbackMap = JavalinEvent.values().associate { it to ArrayList<() -> Unit>() }
    fun fireEvent(javalinEvent: JavalinEvent) = callbackMap[javalinEvent]!!.forEach { callback -> callback.invoke() }
    var handlerAddedCallback: ((HandlerMetaInfo) -> Unit)? = null
    fun fireHandlerAddedEvent(handlerMetaInfo: HandlerMetaInfo) = handlerAddedCallback.apply { this?.invoke(handlerMetaInfo) }
}

class EventAttacher(private val eventManager: EventManager) {
    fun serverStarting(callback: () -> Unit) = eventManager.callbackMap[JavalinEvent.SERVER_STARTING]?.add(callback)
    fun serverStarted(callback: () -> Unit) = eventManager.callbackMap[JavalinEvent.SERVER_STARTED]?.add(callback)
    fun serverStartFailed(callback: () -> Unit) = eventManager.callbackMap[JavalinEvent.SERVER_START_FAILED]?.add(callback)
    fun serverStopping(callback: () -> Unit) = eventManager.callbackMap[JavalinEvent.SERVER_STOPPING]?.add(callback)
    fun serverStopped(callback: () -> Unit) = eventManager.callbackMap[JavalinEvent.SERVER_STOPPED]?.add(callback)
    fun handlerAdded(callback: (HandlerMetaInfo) -> Unit) {
        eventManager.handlerAddedCallback = callback
    }
}

enum class JavalinEvent { SERVER_STARTING, SERVER_STARTED, SERVER_START_FAILED, SERVER_STOPPING, SERVER_STOPPED }
data class HandlerMetaInfo(val httpMethod: HandlerType, val path: String, val handler: Any, val roles: Set<Role>)

