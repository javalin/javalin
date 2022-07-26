/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.jetty

import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.http.JavalinServlet
import io.javalin.http.ServletContext
import io.javalin.http.util.ContextUtil
import io.javalin.websocket.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.session.Session
import org.eclipse.jetty.websocket.api.util.WebSocketConstants
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory
import java.util.function.Consumer

internal const val upgradeContextKey = "javalin-ws-upgrade-context"
internal const val upgradeSessionAttrsKey = "javalin-ws-upgrade-http-session"

/**
 * The [JavalinJettyServlet] is responsible for both WebSocket and HTTP requests.
 * It extends Jetty's [WebSocketServlet], and has a [JavalinServlet] as a constructor arg.
 * It switches between WebSocket and HTTP in the [service] method.
 */
class JavalinJettyServlet(val cfg: JavalinConfig, private val httpServlet: JavalinServlet) : JettyWebSocketServlet() {

    val wsExceptionMapper = WsExceptionMapper()
    val wsPathMatcher = WsPathMatcher()

    fun addHandler(handlerType: WsHandlerType, path: String, ws: Consumer<WsConfig>, roles: Set<io.javalin.security.RouteRole>) {
        wsPathMatcher.add(WsEntry(handlerType, path, cfg.routing, WsConfig().apply { ws.accept(this) }, roles))
    }

    override fun configure(factory: JettyWebSocketServletFactory) { // this is called once, before everything
        cfg.pvt.wsFactoryConfig?.accept(factory)
        factory.setCreator(JettyWebSocketCreator { req, _ -> // this is called when a websocket is created (after [service])
            val preUpgradeContext = req.httpServletRequest.getAttribute(upgradeContextKey) as ServletContext
            req.httpServletRequest.setAttribute(upgradeContextKey, ContextUtil.changeBaseRequest(preUpgradeContext, req.httpServletRequest))
            val session = req.session as? Session?
            req.httpServletRequest.setAttribute(upgradeSessionAttrsKey, session?.attributeNames?.asSequence()?.associateWith { session.getAttribute(it) })
            return@JettyWebSocketCreator WsConnection(wsPathMatcher, wsExceptionMapper, cfg.pvt.wsLogger)
        })
    }

    override fun service(req: HttpServletRequest, res: HttpServletResponse) { // this handles both http and websocket
        if (req.getHeader(Header.SEC_WEBSOCKET_KEY) == null) { // this isn't a websocket request
            return httpServlet.service(req, res) // treat as normal HTTP request
        }
        val requestUri = req.requestURI.removePrefix(req.contextPath)
        val entry = wsPathMatcher.findEndpointHandlerEntry(requestUri) ?: return res.sendError(404, "WebSocket handler not found")
        val upgradeContext = ServletContext(req, res, cfg.pvt.appAttributes).apply {
            pathParamMap = entry.extractPathParams(requestUri)
            matchedPath = entry.path
        }
        if (!allowedByAccessManager(entry, upgradeContext)) return res.sendError(401, "Unauthorized")
        req.setAttribute(upgradeContextKey, upgradeContext)
        setWsProtocolHeader(req, res)
        super.service(req, res) // everything is okay, perform websocket upgrade
    }

    private fun allowedByAccessManager(entry: WsEntry, ctx: Context): Boolean = try {
        cfg.pvt.accessManager.manage({ it.attribute("javalin-ws-upgrade-allowed", true) }, ctx, entry.roles) // run access manager
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

