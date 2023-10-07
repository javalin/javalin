/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.event

import io.javalin.http.Handler
import io.javalin.http.HandlerType
import io.javalin.security.RouteRole
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsHandlerType
import java.util.function.Consumer

class EventManager {

    val lifecycleHandlers = JavalinLifecycleEvent.entries.associateWith { HashSet<LifecycleEventListener>() }
    var handlerAddedHandlers = mutableSetOf<Consumer<HandlerMetaInfo>>()
    val wsHandlerAddedHandlers = mutableSetOf<Consumer<WsHandlerMetaInfo>>()

    fun fireEvent(javalinLifecycleEvent: JavalinLifecycleEvent) = lifecycleHandlers[javalinLifecycleEvent]?.forEach { it.handleEvent() }
    fun fireHandlerAddedEvent(metaInfo: HandlerMetaInfo) = handlerAddedHandlers.onEach { it.accept(metaInfo) }
    fun fireWsHandlerAddedEvent(metaInfo: WsHandlerMetaInfo) = wsHandlerAddedHandlers.onEach { it.accept(metaInfo) }

    internal fun addLifecycleEvent(event: JavalinLifecycleEvent, lifecycleEventListener: LifecycleEventListener) {
        lifecycleHandlers[event]!!.add(lifecycleEventListener)
    }

}

enum class JavalinLifecycleEvent { SERVER_STARTING, SERVER_STARTED, SERVER_START_FAILED, SERVER_STOP_FAILED, SERVER_STOPPING, SERVER_STOPPED }
data class HandlerMetaInfo(val httpMethod: HandlerType, val path: String, val handler: Handler, val roles: Set<RouteRole>)
data class WsHandlerMetaInfo(val handlerType: WsHandlerType, val path: String, val wsConfig: Consumer<WsConfig>, val roles: Set<RouteRole>)
