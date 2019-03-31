/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Javalin
import io.javalin.security.Role
import java.util.*
import java.util.function.Consumer

class EventManager(val parentJavalin: Javalin) {
    val eventAttacher = EventAttacher(this)
    val callbackMap = JavalinEvent.values().associate { it to ArrayList<Runnable>() }
    fun fireEvent(javalinEvent: JavalinEvent) = callbackMap[javalinEvent]!!.forEach { callback -> callback.run() }
    var handlerAddedCallback: (Consumer<HandlerMetaInfo>)? = null
    fun fireHandlerAddedEvent(handlerMetaInfo: HandlerMetaInfo) = handlerAddedCallback.apply { this?.accept(handlerMetaInfo) }
}

enum class JavalinEvent { SERVER_STARTING, SERVER_STARTED, SERVER_START_FAILED, SERVER_STOPPING, SERVER_STOPPED }
data class HandlerMetaInfo(val httpMethod: HandlerType, val path: String, val handler: Any, val roles: Set<Role>)
