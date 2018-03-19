/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty.websocket

import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse
import org.eclipse.jetty.websocket.servlet.WebSocketCreator

class RootWebSocketCreator(private val handlerRoot: WebSocketHandlerRoot, private val javalinWsHandlers: List<WebSocketHandler>) : WebSocketCreator {

    override fun createWebSocket(req: ServletUpgradeRequest, resp: ServletUpgradeResponse): Any {
        if (javalinWsHandlers.find { it.matches(req.requestURI.path) } == null) {
            resp.sendError(404, "Not found")
        }
        return handlerRoot
    }

}