/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.jetty

import io.javalin.core.JavalinConfig
import io.javalin.core.security.RouteRole
import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.JavalinServlet
import io.javalin.http.util.ContextUtil
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsConnection
import io.javalin.websocket.WsEntry
import io.javalin.websocket.WsExceptionMapper
import io.javalin.websocket.WsHandlerType
import io.javalin.websocket.WsPathMatcher
import org.eclipse.jetty.websocket.api.WebSocketConstants
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import org.eclipse.jetty.websocket.servlet.WebSocketServlet
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.util.function.Consumer
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal const val upgradeContextKey = "javalin-ws-upgrade-context"
internal const val upgradeSessionAttrsKey = "javalin-ws-upgrade-http-session"

/**
 * The [JavalinJettyServlet] is responsible for both WebSocket and HTTP requests.
 * It extends Jetty's [WebSocketServlet], and has a [JavalinServlet] as a constructor arg.
 * It switches between WebSocket and HTTP in the [service] method.
 */
class JavalinJettyServlet(val config: JavalinConfig, private val httpServlet: JavalinServlet) : WebSocketServlet() {

    val wsExceptionMapper = WsExceptionMapper()
    val wsPathMatcher = WsPathMatcher()

    fun addHandler(handlerType: WsHandlerType, path: String, ws: Consumer<WsConfig>, roles: Set<RouteRole>) {
        wsPathMatcher.add(WsEntry(handlerType, path, config.ignoreTrailingSlashes, WsConfig().apply { ws.accept(this) }, roles))
    }

    override fun configure(factory: WebSocketServletFactory) { // this is called once, before everything
        config.inner.wsFactoryConfig?.accept(factory)
        factory.creator = WebSocketCreator { req, _ -> // this is called when a websocket is created (after [service])
            val preUpgradeContext = req.httpServletRequest.getAttribute(upgradeContextKey) as Context
            req.httpServletRequest.setAttribute(upgradeContextKey, ContextUtil.changeBaseRequest(preUpgradeContext, req.httpServletRequest))
            req.httpServletRequest.setAttribute(upgradeSessionAttrsKey, req.session?.attributeNames?.asSequence()?.associateWith { req.session.getAttribute(it) })
            return@WebSocketCreator WsConnection(wsPathMatcher, wsExceptionMapper, config.inner.wsLogger)
        }
    }

    override fun service(req: HttpServletRequest, res: HttpServletResponse) { // this handles both http and websocket
        if (req.getHeader(Header.SEC_WEBSOCKET_KEY) == null) { // this isn't a websocket request
            return httpServlet.service(req, res) // treat as normal HTTP request
        }
        val requestUri = req.requestURI.removePrefix(req.contextPath)
        val entry = wsPathMatcher.findEndpointHandlerEntry(requestUri) ?: return res.sendError(404, "WebSocket handler not found")
        val upgradeContext = Context(req, res, config.inner.appAttributes).apply {
            pathParamMap = entry.extractPathParams(requestUri)
            matchedPath = entry.path
        }
        if (!allowedByAccessManager(entry, upgradeContext)) return res.sendError(401, "Unauthorized")
        req.setAttribute(upgradeContextKey, upgradeContext)
        setWsProtocolHeader(req, res)
        super.service(req, res) // everything is okay, perform websocket upgrade
    }

    private fun allowedByAccessManager(entry: WsEntry, ctx: Context): Boolean = try {
        config.inner.accessManager.manage({ it.attribute("javalin-ws-upgrade-allowed", true) }, ctx, entry.roles) // run access manager
        ctx.attribute<Boolean>("javalin-ws-upgrade-allowed") == true // attribute is true if access manger allowed the request
    } catch (e: Exception) {
        false
    }

    private fun setWsProtocolHeader(req: HttpServletRequest, res: HttpServletResponse) {
        val wsProtocolHeader = req.getHeader(WebSocketConstants.SEC_WEBSOCKET_PROTOCOL) ?: return
        val firstProtocol = wsProtocolHeader.split(',').map { it.trim() }.find { it.isNotBlank() } ?: return
        res.setHeader(WebSocketConstants.SEC_WEBSOCKET_PROTOCOL, firstProtocol)
    }

}

