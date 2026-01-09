/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.http.servlet.JavalinWsServletContext
import org.eclipse.jetty.websocket.api.Callback
import org.eclipse.jetty.websocket.api.Session
import java.nio.ByteBuffer

/**
 * Is instantiated for every WebSocket connection. It keeps the session and sessionId and handles incoming events by
 * delegating to the registered before, endpoint, after and logger handlers.
 *
 * Implements [Session.Listener.AutoDemanding] to use Jetty's programmatic WebSocket API instead of annotations (Javalin <7)
 * The AutoDemanding interface signals that WebSocket frames are demanded automatically after each event callback.
 */
class WsConnection(
    private val matcher: WsPathMatcher,
    private val exceptionMapper: WsExceptionMapper,
    private val wsLogger: WsConfig?,
    private val upgradeCtx: JavalinWsServletContext
) : Session.Listener.AutoDemanding {

    private lateinit var session: Session

    override fun onWebSocketOpen(session: Session) {
        this.session = session
        val ctx = WsConnectContext(upgradeCtx.attach(session))
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsConnectHandler?.handleConnect(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsConnectHandler?.handleConnect(ctx) }
        wsLogger?.wsConnectHandler?.handleConnect(ctx)
    }

    override fun onWebSocketText(message: String) {
        val ctx = WsMessageContext(upgradeCtx.attach(session), message)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsMessageHandler?.handleMessage(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsMessageHandler?.handleMessage(ctx) }
        wsLogger?.wsMessageHandler?.handleMessage(ctx)
    }

    override fun onWebSocketBinary(payload: ByteBuffer, callback: Callback) {
        val ctx = WsBinaryMessageContext(upgradeCtx.attach(session), payload)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsBinaryMessageHandler?.handleBinaryMessage(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsBinaryMessageHandler?.handleBinaryMessage(ctx) }
        wsLogger?.wsBinaryMessageHandler?.handleBinaryMessage(ctx)
        callback.succeed()
    }

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        val ctx = WsCloseContext(upgradeCtx.attach(session), statusCode, reason)
        tryBeforeAndEndpointHandlers(ctx) { it.wsConfig.wsCloseHandler?.handleClose(ctx) }
        tryAfterHandlers(ctx) { it.wsConfig.wsCloseHandler?.handleClose(ctx) }
        wsLogger?.wsCloseHandler?.handleClose(ctx)
        ctx.disableAutomaticPings()
    }

    override fun onWebSocketError(cause: Throwable) {
        val ctx = WsErrorContext(upgradeCtx.attach(session), cause)
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
