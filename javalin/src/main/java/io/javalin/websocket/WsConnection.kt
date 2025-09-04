/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

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

/**
 * Is instantiated for every WebSocket connection. It keeps the session and sessionId and handles incoming events by
 * delegating to the registered before, endpoint, after and logger handlers.
 */
@WebSocket
class WsConnection(val matcher: WsPathMatcher, val exceptionMapper: WsExceptionMapper, val wsLogger: WsConfig?) {

    private val sessionId: String = UUID.randomUUID().toString()

    @OnWebSocketOpen
    fun onConnect(session: Session) {
        val ctx = WsConnectContext(sessionId, session)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsConnectHandler?.handleConnect(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsConnectHandler?.handleConnect(ctx) }
        wsLogger?.wsConnectHandler?.handleConnect(ctx)
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
    }

    @OnWebSocketError
    fun onError(session: Session, throwable: Throwable?) {
        val ctx = WsErrorContext(sessionId, session, throwable)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsErrorHandler?.handleError(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsErrorHandler?.handleError(ctx) }
        wsLogger?.wsErrorHandler?.handleError(ctx)
    }

    private fun tryBeforeAndEndpointHandlers(ctx: WsContext, handle: (WsHandlerEntry) -> Unit) {
        val requestUri = ctx.session.uriNoContextPath()
        try {
            matcher.findBeforeHandlerEntries(requestUri).forEach { handle.invoke(it) }
            matcher.findEndpointHandlerEntry(requestUri)!!.let { handle.invoke(it) } // never null, 404 is handled in front
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }
    }

    private fun tryAfterHandlers(ctx: WsContext, handle: (WsHandlerEntry) -> Unit) {
        val requestUri = ctx.session.uriNoContextPath()
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

private fun Session.uriNoContextPath(): String = try {
    this.upgradeRequest.requestURI.path.removePrefix(jettyUpgradeRequest.httpServletRequest.contextPath)
} catch (e: Exception) {
    // Fallback - just use the path without removing context path
    this.upgradeRequest.requestURI.path
}
