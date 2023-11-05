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

/**
 * Class propagating events from the Jetty webserver to any registered listener.
 */
class EventManager {

    val lifecycleHandlers = JavalinLifecycleEvent.entries.associateWith { HashSet<LifecycleEventListener>() }
    var handlerAddedHandlers = mutableSetOf<Consumer<HandlerMetaInfo>>()
    val wsHandlerAddedHandlers = mutableSetOf<Consumer<WsHandlerMetaInfo>>()

    /** Fires a Javalin Lifecycle Event to the listeners. */
    fun fireEvent(javalinLifecycleEvent: JavalinLifecycleEvent) = lifecycleHandlers[javalinLifecycleEvent]?.forEach { it.handleEvent() }
    /** Fires an event telling listeners that a new HTTP handler has been added.*/
    fun fireHandlerAddedEvent(metaInfo: HandlerMetaInfo) = handlerAddedHandlers.onEach { it.accept(metaInfo) }
    /** Fires an event telling listeners that a new WebSocket handler has been added. */
    fun fireWsHandlerAddedEvent(metaInfo: WsHandlerMetaInfo) = wsHandlerAddedHandlers.onEach { it.accept(metaInfo) }

    internal fun addLifecycleEvent(event: JavalinLifecycleEvent, lifecycleEventListener: LifecycleEventListener) {
        lifecycleHandlers[event]!!.add(lifecycleEventListener)
    }

}

/**
 * The possible Javalin lifecycle event
 */
enum class JavalinLifecycleEvent {
    /** Event fired when attempting to start the Jetty Webserver */
    SERVER_STARTING,
    /** Event fired when the Jetty Webserver was started successfully */
    SERVER_STARTED,
    /** Event fired when an exception occurs while starting the Jetty Webserver */
    SERVER_START_FAILED,
    /** Event fired when an exception occurs while stopping the Jetty Webserver */
    SERVER_STOP_FAILED,
    /** Event fired when attempting to stop the Jetty Webserver */
    SERVER_STOPPING,
    /** Event fired after the Jetty Webserver was stopped successfully */
    SERVER_STOPPED
}

/**
 * Metadata information about a HTTP Handler.
 * @param httpMethod the [HandlerType] method (e.g.: GET, POST, …)
 * @param path the path (e.g.: "/home")
 * @param handler the actual [Handler]
 * @param roles the authorization roles
 */
data class HandlerMetaInfo(
    val httpMethod: HandlerType,
    val path: String,
    val handler: Handler,
    val roles: Set<RouteRole>
)

/**
 *  Metadata information about a WebSocket Handler.
 * @param handlerType the [WsHandlerType] method (e.g.: WEBSOCKET_BEFORE, WEBSOCKET, WEBSOCKET_AFTER)
 * @param path the path (e.g.: "/home")
 * @param wsConfig the actual [WsConfig] consumer
 * @param roles the authorization roles
 */
data class WsHandlerMetaInfo(
    val handlerType: WsHandlerType,
    val path: String,
    val wsConfig: Consumer<WsConfig>,
    val roles: Set<RouteRole>
)
