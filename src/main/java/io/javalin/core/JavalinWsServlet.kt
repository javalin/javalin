/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Context
import io.javalin.Javalin
import io.javalin.UnauthorizedResponse
import io.javalin.core.util.Header
import io.javalin.core.util.Util
import io.javalin.security.Role
import io.javalin.websocket.WsEntry
import io.javalin.websocket.WsHandler
import io.javalin.websocket.WsPathMatcher
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.util.*
import java.util.function.Consumer
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JavalinWsServlet(val config: JavalinConfig) : WebSocketServlet() {

    var wsPathMatcher = WsPathMatcher(config)

    override fun configure(factory: WebSocketServletFactory) {
        config.wsFactoryConfig?.accept(factory)
        factory.creator = WebSocketCreator { req, res -> wsPathMatcher } // this is a long-lived object handling multiple connections
    }

    override fun service(req: HttpServletRequest, res: HttpServletResponse) {
        if (req.isWebSocket()) {
            val entry = wsPathMatcher.findEntry(req.requestURI.removePrefix(req.contextPath)) ?: return res.sendError(404, "WebSocket handler not found")
            try {
                val randomUuid = UUID.randomUUID().toString()
                config.accessManager.manage({ it.req.setAttribute("javalin-ws-upgrade-key", randomUuid) }, Context(req, res, mapOf()), entry.permittedRoles)
                if (req.getAttribute("javalin-ws-upgrade-key") != randomUuid) throw UnauthorizedResponse() // if the keys match, the access manager ran the handler (== valid)
                super.service(req, res)
            } catch (e: Exception) {
                res.sendError(401, "Unauthorized")
            }
        } else { // if not websocket (and not handled by http-handler), this request is below the context path
            Util.writeResponse(res, "Not found. Request is below context-path", 404)
            Javalin.log.warn("Received a request below context-path. Returned 404.")
        }
    }

    fun addHandler(path: String, ws: Consumer<WsHandler>, permittedRoles: Set<Role>) {
        wsPathMatcher.add(WsEntry(path, WsHandler().apply { ws.accept(this) }, permittedRoles))
    }
}

fun HttpServletRequest.isWebSocket(): Boolean = this.getHeader(Header.SEC_WEBSOCKET_KEY) != null

