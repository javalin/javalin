/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.util.function.Consumer

class JavalinWsServlet : WebSocketServlet() {

    var wsFactoryConfig: Consumer<WebSocketServletFactory>? = null
    var wsPathMatcher = WsPathMatcher()

    override fun configure(factory: WebSocketServletFactory) {
        wsFactoryConfig?.accept(factory)
        factory.creator = WebSocketCreator { req, res ->
            wsPathMatcher.findEntry(req) ?: res.sendError(404, "WebSocket handler not found")
            wsPathMatcher // this is a long-lived object handling multiple connections
        }
    }

    fun addHandler(path: String, ws: Consumer<WsHandler>) =
            wsPathMatcher.add(WsEntry(path, WsHandler().apply { ws.accept(this) }))

    fun setWsLogger(ws: Consumer<WsHandler>) {
        wsPathMatcher.wsLogger = WsHandler().apply { ws.accept(this) }
    }

}
