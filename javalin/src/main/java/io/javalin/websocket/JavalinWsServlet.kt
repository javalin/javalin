/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.core.JavalinConfig
import io.javalin.core.security.Role
import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.JavalinServlet
import io.javalin.http.UnauthorizedResponse
import io.javalin.http.util.ContextUtil
import org.eclipse.jetty.websocket.api.WebSocketConstants
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.util.function.Consumer
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal const val upgradeAllowedKey = "javalin-ws-upgrade-allowed"
internal const val upgradeContextKey = "javalin-ws-upgrade-context"
internal const val upgradeSessionAttrsKey = "javalin-ws-upgrade-http-session"

/**
 * The JavalinWsServlet is responsible for both WebSocket and HTTP requests.
 * It extends Jetty's WebSocketServlet, and has a HTTP Servlet as a constructor arg.
 * It switches between WebSocket and HTTP in the `service` method.
 */
class JavalinWsServlet(val config: JavalinConfig, private val httpServlet: JavalinServlet) : WebSocketServlet() {

    val wsExceptionMapper = WsExceptionMapper()

    private val wsPathMatcher = WsPathMatcher()

    override fun configure(factory: WebSocketServletFactory) {
        config.inner.wsFactoryConfig?.accept(factory)
        factory.creator = WebSocketCreator { req, res ->
            val preUpgradeContext = req.httpServletRequest.getAttribute(upgradeContextKey) as Context
            req.httpServletRequest.setAttribute(upgradeContextKey, ContextUtil.changeBaseRequest(preUpgradeContext, req.httpServletRequest))
            req.httpServletRequest.setAttribute(upgradeSessionAttrsKey, req.session?.attributeNames?.asSequence()?.associateWith { req.session.getAttribute(it) })
            return@WebSocketCreator WsHandlerController(wsPathMatcher, wsExceptionMapper, config.inner.wsLogger)
        }
    }

    override fun service(req: HttpServletRequest, res: HttpServletResponse) { // this handles both http and websocket
        if (!req.isWebSocket()) {
            return httpServlet.service(req, res)
        }
        val requestUri = req.requestURI.removePrefix(req.contextPath)
        val entry = wsPathMatcher.findEndpointHandlerEntry(requestUri) ?: return res.sendError(404, "WebSocket handler not found")
        try {
            val upgradeContext = Context(req, res).apply {
                pathParamMap = entry.extractPathParams(requestUri)
                matchedPath = entry.path
            }
            config.inner.accessManager.manage({ ctx -> ctx.req.setAttribute(upgradeAllowedKey, true) }, upgradeContext, entry.permittedRoles)
            if (req.getAttribute(upgradeAllowedKey) != true) throw UnauthorizedResponse() // if set to true, the access manager ran the handler (== valid)
            req.setAttribute(upgradeContextKey, upgradeContext)

            val wsProtocolHeader = req.getHeader(WebSocketConstants.SEC_WEBSOCKET_PROTOCOL)
            if (wsProtocolHeader != null) {
                val protocolNames = wsProtocolHeader.split(',').map { it.trim() }.filter { it != "" }
                if (protocolNames.isNotEmpty()) {
                    res.setHeader(WebSocketConstants.SEC_WEBSOCKET_PROTOCOL, protocolNames.first())
                }
            }

            super.service(req, res) // everything is okay, perform websocket upgrade
        } catch (e: Exception) {
            res.sendError(401, "Unauthorized")
        }
    }

    fun addHandler(handlerType: WsHandlerType, path: String, ws: Consumer<WsHandler>, permittedRoles: Set<Role>) {
        wsPathMatcher.add(WsEntry(handlerType, path, config.ignoreTrailingSlashes, WsHandler().apply { ws.accept(this) }, permittedRoles))
    }
}

fun HttpServletRequest.isWebSocket(): Boolean = this.getHeader(Header.SEC_WEBSOCKET_KEY) != null

