/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.Javalin
import io.javalin.core.util.Header
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.io.ByteArrayInputStream
import java.util.function.Consumer
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinWsServlet : WebSocketServlet() {

    var config = JavalinWsServletConfig(this)
    var wsPathMatcher = WsPathMatcher()

    override fun configure(factory: WebSocketServletFactory) {
        config.wsFactoryConfig?.accept(factory)
        factory.creator = WebSocketCreator { req, res ->
            wsPathMatcher.findEntry(req) ?: res.sendError(404, "WebSocket handler not found")
            wsPathMatcher // this is a long-lived object handling multiple connections
        }
    }

    override fun service(req: ServletRequest?, res: ServletResponse?) {
        if ((req as HttpServletRequest).isWebSocket()) return super.service(req, res) // handle normally
        // if not handled by http handler, and not websocket, this request is below the context path of the http handler.
        val response = res as HttpServletResponse
        response.status = 404
        ByteArrayInputStream("Not found. Request is below context-path".toByteArray()).copyTo(response.outputStream)
        response.outputStream.close()
        Javalin.log.warn("Received a request below context-path. Returned 404.")
    }

    fun addHandler(path: String, ws: Consumer<WsHandler>) = wsPathMatcher.add(WsEntry(path, WsHandler().apply { ws.accept(this) }))
}

fun HttpServletRequest.isWebSocket(): Boolean = this.getHeader(Header.SEC_WEBSOCKET_KEY) != null
