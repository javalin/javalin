/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.http.servlet.JavalinWsServletContext
import org.eclipse.jetty.websocket.api.Callback
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketOpen
import org.eclipse.jetty.websocket.api.annotations.WebSocket
import java.nio.ByteBuffer

/**
 * Is instantiated for every WebSocket connection. It keeps the session and sessionId and handles incoming events by
 * delegating to the registered before, endpoint, after and logger handlers.
 */
@WebSocket
class WsConnection(
    private val matcher: WsPathMatcher,
    private val exceptionMapper: WsExceptionMapper,
    private val wsLogger: WsConfig?,
    private val upgradeCtx: JavalinWsServletContext
) {

    @OnWebSocketOpen
    fun onConnect(session: Session) {
        val ctx = WsConnectContext(upgradeCtx.attach(session))
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsConnectHandler?.handleConnect(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsConnectHandler?.handleConnect(ctx) }
        wsLogger?.wsConnectHandler?.handleConnect(ctx)
    }

    @OnWebSocketMessage
    fun onMessage(session: Session, message: String) {
        val ctx = WsMessageContext(upgradeCtx.attach(session), message)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsMessageHandler?.handleMessage(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsMessageHandler?.handleMessage(ctx) }
        wsLogger?.wsMessageHandler?.handleMessage(ctx)
    }

    @OnWebSocketMessage
    fun onMessage(session: Session, data: ByteBuffer, callback: Callback) {
        val ctx = WsBinaryMessageContext(upgradeCtx.attach(session), data)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsBinaryMessageHandler?.handleBinaryMessage(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsBinaryMessageHandler?.handleBinaryMessage(ctx) }
        wsLogger?.wsBinaryMessageHandler?.handleBinaryMessage(ctx)
        callback.succeed()
    }

    @OnWebSocketClose
    fun onClose(session: Session, statusCode: Int, reason: String?) {
        val ctx = WsCloseContext(upgradeCtx.attach(session), statusCode, reason)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsCloseHandler?.handleClose(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsCloseHandler?.handleClose(ctx) }
        wsLogger?.wsCloseHandler?.handleClose(ctx)
        ctx.disableAutomaticPings()
    }

    @OnWebSocketError
    fun onError(session: Session, throwable: Throwable?) {
        val ctx = WsErrorContext(upgradeCtx.attach(session), throwable)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsErrorHandler?.handleError(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsErrorHandler?.handleError(ctx) }
        wsLogger?.wsErrorHandler?.handleError(ctx)
    }

    private fun tryBeforeAndEndpointHandlers(ctx: WsContext, handle: (WsHandlerEntry) -> Unit) {
        val requestUri = upgradeCtx.extractedData.requestUri
        try {
            matcher.findBeforeHandlerEntries(requestUri).forEach { handle.invoke(it) }
            matcher.findEndpointHandlerEntry(requestUri)!!.let { handle.invoke(it) } // never null, 404 is handled in front
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }
    }

    private fun tryAfterHandlers(ctx: WsContext, handle: (WsHandlerEntry) -> Unit) {
        val requestUri = upgradeCtx.extractedData.requestUri
        try {
            matcher.findAfterHandlerEntries(requestUri).forEach { handle.invoke(it) }
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }
    }

}
