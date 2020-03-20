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
import io.javalin.http.UnauthorizedResponse
import io.javalin.http.util.ContextUtil
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.server.NativeWebSocketConfiguration
import org.eclipse.jetty.websocket.server.WebSocketServerFactory
import org.eclipse.jetty.websocket.server.WebSocketUpgradeFilter
import org.eclipse.jetty.websocket.servlet.WebSocketCreator
import java.util.function.Consumer
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

internal const val upgradeAllowedKey = "javalin-ws-upgrade-allowed"
internal const val upgradeContextKey = "javalin-ws-upgrade-context"
internal const val upgradeHttpSessionKey = "javalin-ws-upgrade-http-session"

class JavalinWsFilterParent(val config: JavalinConfig) {

    val wsExceptionMapper = WsExceptionMapper()
    val wsPathMatcher = WsPathMatcher()

    fun createWsFilter(servletContextHandler: ServletContextHandler): JavalinWsUpgradeFilter {
        val wsFactory = WebSocketServerFactory(servletContextHandler.servletContext)
        config.inner.wsFactoryConfig?.accept(wsFactory)
        wsFactory.creator = WebSocketCreator { req, res ->
            val preUpgradeContext = req.httpServletRequest.getAttribute(upgradeContextKey) as Context // the base request is GC'ed fast, so we have to switch
            req.httpServletRequest.setAttribute(upgradeContextKey, ContextUtil.changeBaseRequest(preUpgradeContext, req.httpServletRequest))
            req.httpServletRequest.setAttribute(upgradeHttpSessionKey, req.session)
            return@WebSocketCreator WsHandlerController(wsPathMatcher, wsExceptionMapper, config.inner.wsLogger)
        }
        val upgradeFilter = JavalinWsUpgradeFilter(wsPathMatcher, config, wsFactory)
        servletContextHandler.servletContext.setAttribute(NativeWebSocketConfiguration::class.java.name, upgradeFilter.configuration)
        return upgradeFilter
    }

    fun addHandler(handlerType: WsHandlerType, path: String, ws: Consumer<WsHandler>, permittedRoles: Set<Role>) {
        wsPathMatcher.add(WsEntry(handlerType, path, WsHandler().apply { ws.accept(this) }, permittedRoles))
    }

}

class JavalinWsUpgradeFilter(val wsPathMatcher: WsPathMatcher, val config: JavalinConfig, wsFactory: WebSocketServerFactory) : WebSocketUpgradeFilter(wsFactory) {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val req by lazy { request as HttpServletRequest }
        val res by lazy { response as HttpServletResponse }
        if (!req.isWebSocket()) return chain.doFilter(request, response)
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
            super.getFactory().acceptWebSocket(req, res)
        } catch (e: Exception) {
            res.sendError(401, "Unauthorized")
        }
    }

}

fun HttpServletRequest.isWebSocket(): Boolean = this.getHeader(Header.SEC_WEBSOCKET_KEY) != null

