/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.jetty

import io.javalin.config.JavalinConfig
import io.javalin.http.HandlerType
import io.javalin.http.Header
import io.javalin.http.servlet.JavalinServlet
import io.javalin.http.servlet.JavalinServletContext
import io.javalin.http.servlet.JavalinServletContextConfig
import io.javalin.http.servlet.Task
import io.javalin.util.javalinLazy
import io.javalin.websocket.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.session.Session
import org.eclipse.jetty.websocket.api.util.WebSocketConstants
import org.eclipse.jetty.websocket.server.JettyWebSocketCreator
import org.eclipse.jetty.websocket.server.JettyWebSocketServlet
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory

internal const val upgradeContextKey = "javalin-ws-upgrade-context"
internal const val upgradeSessionAttrsKey = "javalin-ws-upgrade-http-session"

/**
 * The [JavalinJettyServlet] is responsible for both WebSocket and HTTP requests.
 * It extends Jetty's [JettyWebSocketServlet], and has a [JavalinServlet] as a constructor arg.
 * It switches between WebSocket and HTTP in the [service] method.
 */
class JavalinJettyServlet(val cfg: JavalinConfig) : JettyWebSocketServlet() {

    private val httpServlet = JavalinServlet(cfg)
    private val servletContextConfig by javalinLazy { JavalinServletContextConfig.of(cfg) }

    override fun configure(factory: JettyWebSocketServletFactory) { // this is called once, before everything
        cfg.pvt.jetty.wsFactoryConfigs.forEach{ it.accept(factory) }
        factory.setCreator(JettyWebSocketCreator { req, _ -> // this is called when a websocket is created (after [service])
            val preUpgradeContext = req.httpServletRequest.getAttribute(upgradeContextKey) as JavalinServletContext
            req.httpServletRequest.setAttribute(upgradeContextKey, preUpgradeContext.changeBaseRequest(req.httpServletRequest))
            val session = req.session as? Session?
            req.httpServletRequest.setAttribute(upgradeSessionAttrsKey, session?.attributeNames?.asSequence()?.associateWith { session.getAttribute(it) })
            return@JettyWebSocketCreator WsConnection(cfg.pvt.wsRouter.wsPathMatcher, cfg.pvt.wsRouter.wsExceptionMapper, cfg.pvt.wsLogger)
        })
    }

    override fun service(req: HttpServletRequest, res: HttpServletResponse) { // this handles both http and websocket
        if (req.getHeader(Header.SEC_WEBSOCKET_KEY) == null) { // this isn't a websocket request
            return httpServlet.service(req, res) // treat as normal HTTP request
        }
        val requestUri = req.requestURI.removePrefix(req.contextPath)
        val entry = cfg.pvt.wsRouter.wsPathMatcher.findEndpointHandlerEntry(requestUri) ?: return res.sendError(404, "WebSocket handler not found")
        val upgradeContext = JavalinServletContext(
            cfg = servletContextConfig,
            req = req,
            res = res,
            matchedPath = entry.path,
            pathParamMap = entry.extractPathParams(requestUri),
        )
        upgradeContext.setRouteRoles(entry.roles) // set roles for the matched handler
        req.setAttribute(upgradeContextKey, upgradeContext)
        setWsProtocolHeader(req, res)
        // add before handlers
        cfg.pvt.internalRouter.findHttpHandlerEntries(HandlerType.WEBSOCKET_BEFORE_UPGRADE, requestUri)
            .forEach { handler -> upgradeContext.tasks.offer(Task { handler.handle(upgradeContext, requestUri) }) }
        // add actual upgrade handler
        upgradeContext.tasks.offer(Task { super.service(req, res) })
        // add after handlers
        cfg.pvt.internalRouter.findHttpHandlerEntries(HandlerType.WEBSOCKET_AFTER_UPGRADE, requestUri)
            .forEach { handler -> upgradeContext.tasks.offer(Task { handler.handle(upgradeContext, requestUri) }) }

        while (upgradeContext.tasks.isNotEmpty()) {
            try {
                val task = upgradeContext.tasks.poll()
                task.handler.handle()
            } catch (e: Exception) {
                cfg.pvt.internalRouter.handleHttpException(upgradeContext, e)
                break
            }
        }
    }

    private fun setWsProtocolHeader(req: HttpServletRequest, res: HttpServletResponse) {
        val wsProtocolHeader = req.getHeader(WebSocketConstants.SEC_WEBSOCKET_PROTOCOL) ?: return
        val firstProtocol = wsProtocolHeader.split(',').map { it.trim() }.find { it.isNotBlank() } ?: return
        res.setHeader(WebSocketConstants.SEC_WEBSOCKET_PROTOCOL, firstProtocol)
    }

}
