/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty.websocket

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse
import org.eclipse.jetty.websocket.servlet.WebSocketCreator

class JettyWebSocketCreator(internal val handler: Any) : WebSocketCreator {
    override fun createWebSocket(request: ServletUpgradeRequest, response: ServletUpgradeResponse): Any {
        return handler
    }
}

class RootWebSocketCreator(private val handlerRoot: WebSocketHandlerRoot, private val javalinWsHandlers: List<WebSocketHandler>) : WebSocketCreator {
    override fun createWebSocket(req: ServletUpgradeRequest, response: ServletUpgradeResponse): Any {
        val handler: WebSocketHandler? = javalinWsHandlers.find { it.matches(req.requestURI.path) }

        if (handler == null) {
            response.sendError(404, "WebSocket handler not found")
        }else {
            handler.updateParams(req.requestURI.path)
        }
        return handlerRoot
    }
}
