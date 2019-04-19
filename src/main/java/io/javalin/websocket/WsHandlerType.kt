/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

/**
 * The available WebSocket handler types.
 */
enum class WsHandlerType {
    WS_BEFORE, WEBSOCKET, WS_AFTER
}
