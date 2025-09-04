/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.jetty.upgradeContextKey
import org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest
import org.eclipse.jetty.util.BufferUtil
import org.eclipse.jetty.websocket.api.Callback
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Is instantiated for every WebSocket connection. It keeps the session and sessionId and handles incoming events by
 * delegating to the registered before, endpoint, after and logger handlers.
 */
@WebSocket
class WsConnection(val matcher: WsPathMatcher, val exceptionMapper: WsExceptionMapper, val wsLogger: WsConfig?) {

    companion object {
        // Global storage for path information keyed by session hashCode
        private val sessionPathInfo = ConcurrentHashMap<Int, PathInfo>()
        
        data class PathInfo(val matchedPath: String, val pathParams: Map<String, String>)
        
        private fun storePathInfo(session: Session, matchedPath: String, pathParams: Map<String, String>) {
            sessionPathInfo[session.hashCode()] = PathInfo(matchedPath, pathParams)
        }
        
        internal fun getPathInfo(session: Session): PathInfo? {
            return sessionPathInfo[session.hashCode()]
        }
        
        internal fun clearPathInfo(session: Session) {
            sessionPathInfo.remove(session.hashCode())
        }
    }

    private val sessionId: String = UUID.randomUUID().toString()

    @OnWebSocketOpen
    fun onConnect(session: Session) {
        // Store path information for later access
        val requestUri = getRequestUri(session)
        val entry = matcher.findEndpointHandlerEntry(requestUri)
        
        // Store the matched path and path params in companion object storage
        storePathInfo(session, entry?.path ?: requestUri, entry?.extractPathParams(requestUri) ?: emptyMap())
        
        val ctx = WsConnectContext(sessionId, session)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsConnectHandler?.handleConnect(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsConnectHandler?.handleConnect(ctx) }
        wsLogger?.wsConnectHandler?.handleConnect(ctx)
    }
    
    private fun getRequestUri(session: Session): String {
        return try {
            // Try to get the request URI that was already processed by the servlet
            val upgradeRequest = session.upgradeRequest as? org.eclipse.jetty.ee10.websocket.server.JettyServerUpgradeRequest
            val context = upgradeRequest?.httpServletRequest?.getAttribute(upgradeContextKey) as? io.javalin.http.servlet.JavalinServletContext
            context?.path() ?: session.uriNoContextPath()
        } catch (e: Exception) {
            // Fallback to the legacy approach
            session.uriNoContextPath()
        }
    }

    @OnWebSocketMessage
    fun onMessage(session: Session, message: String) {
        val ctx = WsMessageContext(sessionId, session, message)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsMessageHandler?.handleMessage(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsMessageHandler?.handleMessage(ctx) }
        wsLogger?.wsMessageHandler?.handleMessage(ctx)
    }

    @OnWebSocketMessage
    fun onMessage(session: Session, buffer: ByteBuffer, callback: Callback) {
        // FIXME: should utilize the ByteBuffer instead of ByteArray
        val data = BufferUtil.toArray(buffer)
        val ctx = WsBinaryMessageContext(sessionId, session, data, 0, data.size)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsBinaryMessageHandler?.handleBinaryMessage(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsBinaryMessageHandler?.handleBinaryMessage(ctx) }
        wsLogger?.wsBinaryMessageHandler?.handleBinaryMessage(ctx)
        callback.succeed()
    }

    @OnWebSocketClose
    fun onClose(session: Session, statusCode: Int, reason: String?) {
        val ctx = WsCloseContext(sessionId, session, statusCode, reason)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsCloseHandler?.handleClose(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsCloseHandler?.handleClose(ctx) }
        wsLogger?.wsCloseHandler?.handleClose(ctx)
        ctx.disableAutomaticPings()
        ctx.cleanup() // Clean up session attributes after all handlers are done
        clearPathInfo(session) // Clean up path information
    }

    @OnWebSocketError
    fun onError(session: Session, throwable: Throwable?) {
        val ctx = WsErrorContext(sessionId, session, throwable)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsErrorHandler?.handleError(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsErrorHandler?.handleError(ctx) }
        wsLogger?.wsErrorHandler?.handleError(ctx)
    }

    private fun tryBeforeAndEndpointHandlers(ctx: WsContext, handle: (WsHandlerEntry) -> Unit) {
        // Get the request URI using the same logic as onConnect
        val requestUri = getRequestUri(ctx.session)
        
        try {
            matcher.findBeforeHandlerEntries(requestUri).forEach { handle.invoke(it) }
            val endpointHandler = matcher.findEndpointHandlerEntry(requestUri)
            if (endpointHandler != null) {
                handle.invoke(endpointHandler)
            } else {
                throw RuntimeException("No WebSocket endpoint handler found for: $requestUri")
            }
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }
    }

    private fun tryAfterHandlers(ctx: WsContext, handle: (WsHandlerEntry) -> Unit) {
        val requestUri = getRequestUri(ctx.session)
        try {
            matcher.findAfterHandlerEntries(requestUri).forEach { handle.invoke(it) }
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }
    }

}

internal val Session.jettyUpgradeRequest: JettyServerUpgradeRequest
    get() = try {
        this.upgradeRequest as JettyServerUpgradeRequest
    } catch (e: ClassCastException) {
        throw RuntimeException("Failed to cast upgradeRequest to JettyServerUpgradeRequest. Actual type: ${this.upgradeRequest?.javaClass?.name}", e)
    }

private fun Session.uriNoContextPath(): String {
    val fullPath = this.upgradeRequest.requestURI.path
    try {
        val contextPath = jettyUpgradeRequest.httpServletRequest.contextPath
        return fullPath.removePrefix(contextPath ?: "")
    } catch (e: Exception) {
        // If we can't access the context path, assume it's a root context
        // Only strip known context paths if they exist
        return fullPath
    }
}
