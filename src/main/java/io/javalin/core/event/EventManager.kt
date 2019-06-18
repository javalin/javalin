/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.event

import io.javalin.core.security.Role
import io.javalin.http.HandlerType
import io.javalin.websocket.WsHandlerType
import java.util.*
import java.util.function.Consumer

class EventManager {
    val lifecycleHandlers = JavalinEvent.values().associate { it to HashSet<EventHandler>() }
    var handlerAddedHandlers = mutableSetOf<Consumer<HandlerMetaInfo>>()
    val wsHandlerAddedHandlers = mutableSetOf<Consumer<WsHandlerMetaInfo>>()
    fun fireEvent(javalinEvent: JavalinEvent) = lifecycleHandlers[javalinEvent]?.forEach { eventHandler -> eventHandler.handleEvent() }
    fun fireHandlerAddedEvent(metaInfo: HandlerMetaInfo) = handlerAddedHandlers.apply { this.forEach { it.accept(metaInfo) } }
    fun fireWsHandlerAddedEvent(metaInfo: WsHandlerMetaInfo) = wsHandlerAddedHandlers.apply { this.forEach { it.accept(metaInfo) } }
}

enum class JavalinEvent { SERVER_STARTING, SERVER_STARTED, SERVER_START_FAILED, SERVER_STOPPING, SERVER_STOPPED }
data class HandlerMetaInfo(val httpMethod: HandlerType, val path: String, val handler: Any, val roles: Set<Role>)
data class WsHandlerMetaInfo(val handlerType: WsHandlerType, val path: String, val handler: Any, val roles: Set<Role>)
