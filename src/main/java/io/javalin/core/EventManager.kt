/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.core.security.Role
import io.javalin.http.HandlerType
import io.javalin.websocket.WsHandlerType
import java.util.*
import java.util.function.Consumer

class EventManager {
    val callbackMap = JavalinEvent.values().associate { it to HashSet<Runnable>() }
    fun fireEvent(javalinEvent: JavalinEvent) = callbackMap[javalinEvent]!!.forEach { callback -> callback.run() }
    var handlerAddedCallbacks = mutableSetOf<Consumer<HandlerMetaInfo>>()
    fun fireHandlerAddedEvent(handlerMetaInfo: HandlerMetaInfo) = handlerAddedCallbacks.apply { this.forEach { it.accept(handlerMetaInfo) } }

    val wsHandlerAddedCallbacks = mutableSetOf<Consumer<WsHandlerMetaInfo>>()
    fun fireWsHandlerAddedEvent(wsHandlerMetaInfo: WsHandlerMetaInfo) =
            wsHandlerAddedCallbacks.apply { this.forEach { it.accept(wsHandlerMetaInfo) } }
}

enum class JavalinEvent { SERVER_STARTING, SERVER_STARTED, SERVER_START_FAILED, SERVER_STOPPING, SERVER_STOPPED }
data class HandlerMetaInfo(val httpMethod: HandlerType, val path: String, val handler: Any, val roles: Set<Role>)
data class WsHandlerMetaInfo(val handlerType: WsHandlerType, val path: String, val handler: Any, val roles: Set<Role>)
