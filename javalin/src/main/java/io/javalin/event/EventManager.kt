/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.event

import io.javalin.http.HandlerType
import io.javalin.security.RouteRole
import io.javalin.websocket.WsHandlerType
import java.util.function.Consumer

class EventManager {
    val lifecycleHandlers = JavalinEvent.values().associateWith { HashSet<EventHandler>() }
    var handlerAddedHandlers = mutableSetOf<Consumer<HandlerMetaInfo>>()
    val wsHandlerAddedHandlers = mutableSetOf<Consumer<WsHandlerMetaInfo>>()
    fun fireEvent(javalinEvent: JavalinEvent) = lifecycleHandlers[javalinEvent]?.forEach { it.handleEvent() }
    fun fireHandlerAddedEvent(metaInfo: HandlerMetaInfo) = handlerAddedHandlers.onEach { it.accept(metaInfo) }
    fun fireWsHandlerAddedEvent(metaInfo: WsHandlerMetaInfo) = wsHandlerAddedHandlers.onEach { it.accept(metaInfo) }
}

enum class JavalinEvent { SERVER_STARTING, SERVER_STARTED, SERVER_START_FAILED, SERVER_STOP_FAILED, SERVER_STOPPING, SERVER_STOPPED }
data class HandlerMetaInfo(val httpMethod: HandlerType, val path: String, val handler: Any, val roles: Set<RouteRole>)
data class WsHandlerMetaInfo(val handlerType: WsHandlerType, val path: String, val handler: Any, val roles: Set<RouteRole>)
