/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.HandlerMetaInfo
import io.javalin.JavalinEvent
import java.util.*
import java.util.function.Consumer

class EventManager {
    val callbackMap = JavalinEvent.values().associate { it to ArrayList<Runnable>() }
    fun fireEvent(javalinEvent: JavalinEvent) = callbackMap[javalinEvent]!!.forEach { callback -> callback.run() }

    var handlerAddedCallback: Consumer<HandlerMetaInfo>? = null
    fun fireHandlerAddedEvent(handlerMetaInfo: HandlerMetaInfo) = handlerAddedCallback.apply { this?.accept(handlerMetaInfo) }
}
