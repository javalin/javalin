/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty.websocket

import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse
import org.eclipse.jetty.websocket.servlet.WebSocketCreator

class CustomWebSocketCreator(internal val handler: WebSocketAdapter) : WebSocketCreator {
    override fun createWebSocket(request: ServletUpgradeRequest, response: ServletUpgradeResponse): WebSocketAdapter {
        return handler
    }
}
