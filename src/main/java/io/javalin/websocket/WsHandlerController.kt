/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A special WebSocket handler which delegates the handling of the specific WebSocket event to the
 * matching custom handlers.
 */
@WebSocket
class WsHandlerController(val matcher: WsPathMatcher, val exceptionMapper: WsExceptionMapper, val wsLogger: WsHandler?) {

    private val sessionIds = ConcurrentHashMap<Session, String>()

    @OnWebSocketConnect
    fun onConnect(session: Session) {
        sessionIds[session] = UUID.randomUUID().toString() // associate a unique ID with this new session
        val ctx = WsConnectContext(sessionIds[session]!!, session)
        tryBeforeAndEndpointHandlers(ctx) { it.handler.wsConnectHandler?.handleConnect(ctx) }
        tryAfterHandlers(ctx) { it.handler.wsConnectHandler?.handleConnect(ctx) }
        wsLogger?.wsConnectHandler?.handleConnect(ctx)
    }

    @OnWebSocketMessage
    fun onMessage(session: Session, message: String) {
        val ctx = WsMessageContext(sessionIds[session]!!, session, message)
        tryBeforeAndEndpointHandlers(ctx) { it.handler.wsMessageHandler?.handleMessage(ctx) }
        tryAfterHandlers(ctx) { it.handler.wsMessageHandler?.handleMessage(ctx) }
        wsLogger?.wsMessageHandler?.handleMessage(ctx)
    }

    @OnWebSocketMessage
    fun onMessage(session: Session, buffer: ByteArray, offset: Int, length: Int) {
        val ctx = WsBinaryMessageContext(sessionIds[session]!!, session, buffer, offset, length)
        tryBeforeAndEndpointHandlers(ctx) { it.handler.wsBinaryMessageHandler?.handleBinaryMessage(ctx) }
        tryAfterHandlers(ctx) { it.handler.wsBinaryMessageHandler?.handleBinaryMessage(ctx) }
        wsLogger?.wsBinaryMessageHandler?.handleBinaryMessage(ctx)
    }

    @OnWebSocketClose
    fun onClose(session: Session, statusCode: Int, reason: String?) {
        val ctx = WsCloseContext(sessionIds[session]!!, session, statusCode, reason)
        tryBeforeAndEndpointHandlers(ctx) { it.handler.wsCloseHandler?.handleClose(ctx) }
        tryAfterHandlers(ctx) { it.handler.wsCloseHandler?.handleClose(ctx) }
        wsLogger?.wsCloseHandler?.handleClose(ctx)
        sessionIds.remove(session) // the socket has been closed, we no longer need to keep track of the session ID
    }

    @OnWebSocketError
    fun onError(session: Session, throwable: Throwable?) {
        val ctx = WsErrorContext(sessionIds[session]!!, session, throwable)
        tryBeforeAndEndpointHandlers(ctx) { it.handler.wsErrorHandler?.handleError(ctx) }
        tryAfterHandlers(ctx) { it.handler.wsErrorHandler?.handleError(ctx) }
        wsLogger?.wsErrorHandler?.handleError(ctx)
    }

    private fun tryBeforeAndEndpointHandlers(ctx: WsContext, handle: (WsEntry) -> Unit) {
        val requestUri = ctx.session.uriNoContextPath()
        try {
            matcher.findBeforeHandlerEntries(requestUri).forEach { handle.invoke(it) }
            matcher.findEndpointHandlerEntry(requestUri)!!.let { handle.invoke(it) } // never null, 404 is handled in front
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }
    }

    private fun tryAfterHandlers(ctx: WsContext, handle: (WsEntry) -> Unit) {
        val requestUri = ctx.session.uriNoContextPath()
        try {
            matcher.findAfterHandlerEntries(requestUri).forEach { handle.invoke(it) }
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }
    }

}

private fun Session.uriNoContextPath() = this.upgradeRequest.requestURI.path.removePrefix((this.upgradeRequest as ServletUpgradeRequest).httpServletRequest.contextPath)
