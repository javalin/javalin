/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Javalin
import io.javalin.core.util.Header
import io.javalin.core.util.Util
import io.javalin.websocket.WsEntry
import io.javalin.websocket.WsHandler
import io.javalin.websocket.WsPathMatcher
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.util.function.Consumer
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinWsServlet(val config: JavalinConfig) : WebSocketServlet() {

    var wsPathMatcher = WsPathMatcher(config)

    override fun configure(factory: WebSocketServletFactory) {
        config.wsFactoryConfig?.accept(factory)
        factory.creator = WebSocketCreator { req, res ->
            wsPathMatcher.findEntry(req) ?: res.sendError(404, "WebSocket handler not found")
            wsPathMatcher // this is a long-lived object handling multiple connections
        }
    }

    override fun service(req: ServletRequest?, res: ServletResponse?) = if ((req as HttpServletRequest).isWebSocket()) {
        super.service(req, res) // if websocket, handle normally
    } else { // if not websocket (and not handled by http-handler), this request is below the context path
        Util.writeResponse(res as HttpServletResponse, "Not found. Request is below context-path", 404)
        Javalin.log.warn("Received a request below context-path. Returned 404.")
    }

    fun addHandler(path: String, ws: Consumer<WsHandler>) = wsPathMatcher.add(WsEntry(path, WsHandler().apply { ws.accept(this) }))
}

fun HttpServletRequest.isWebSocket(): Boolean = this.getHeader(Header.SEC_WEBSOCKET_KEY) != null
