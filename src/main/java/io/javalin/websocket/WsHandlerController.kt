/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.*
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * A special WebSocket handler which delegates the handling of the specific WebSocket event to the
 * matching custom handlers.
 */
@WebSocket
class WsHandlerController(private val matcher: WsPathMatcher,
                          private val exceptionMapper: WsExceptionMapper,
                          private val wsLogger: WsHandler?) {

    private val sessionIds = ConcurrentHashMap<Session, String>()

    @OnWebSocketConnect
    fun onConnect(session: Session) {
        // Associate a unique ID with this new session
        val sessionId = UUID.randomUUID().toString()
        sessionIds[session] = sessionId

        val requestUri = session.uriNoContextPath()
        val ctx = WsConnectContext(sessionId, session)

        try {
            // Invoke before handlers (if any)
            matcher.findBeforeHandlerEntries(requestUri).forEach { it.handler.connectHandler?.handleConnect(ctx) }

            // Invoke endpoint handler (can't be null, otherwise the ws servlet would not have invoked the controller)
            matcher.findEndpointHandlerEntry(requestUri)!!.let { it.handler.connectHandler?.handleConnect(ctx) }
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }

        // Invoke after handlers (if any)
        try {
            matcher.findAfterHandlerEntries(requestUri).forEach { it.handler.connectHandler?.handleConnect(ctx) }
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }

        wsLogger?.connectHandler?.handleConnect(ctx)
    }

    @OnWebSocketMessage
    fun onMessage(session: Session, message: String) {
        val requestUri = session.uriNoContextPath()
        val ctx = WsMessageContext(sessionIds[session]!!, session, message)

        try {
            // Invoke before handlers (if any)
            matcher.findBeforeHandlerEntries(requestUri).forEach { it.handler.messageHandler?.handleMessage(ctx) }

            // Invoke endpoint handler (can't be null, otherwise the ws servlet would not have invoked the controller)
            matcher.findEndpointHandlerEntry(requestUri)!!.let { it.handler.messageHandler?.handleMessage(ctx) }
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }

        // Invoke after handlers (if any)
        try {
            matcher.findAfterHandlerEntries(requestUri).forEach { it.handler.messageHandler?.handleMessage(ctx) }
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }

        wsLogger?.messageHandler?.handleMessage(ctx)
    }

    @OnWebSocketMessage
    fun onMessage(session: Session, buffer: ByteArray, offset: Int, length: Int) {
        val requestUri = session.uriNoContextPath()
        val ctx = WsBinaryMessageContext(sessionIds[session]!!, session, buffer.toTypedArray(), offset, length)

        try {
            // Invoke before handlers (if any)
            matcher.findBeforeHandlerEntries(requestUri).forEach { it.handler.binaryMessageHandler?.handleBinaryMessage(ctx) }

            // Invoke endpoint handler (can't be null, otherwise the ws servlet would not have invoked the controller)
            matcher.findEndpointHandlerEntry(requestUri)!!.let { it.handler.binaryMessageHandler?.handleBinaryMessage(ctx) }
        } catch (e: java.lang.Exception) {
            exceptionMapper.handle(e, ctx)
        }

        // Invoke after handlers (if any)
        try {
            matcher.findAfterHandlerEntries(requestUri).forEach { it.handler.binaryMessageHandler?.handleBinaryMessage(ctx) }
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }

        wsLogger?.binaryMessageHandler?.handleBinaryMessage(ctx)
    }

    @OnWebSocketClose
    fun onClose(session: Session, statusCode: Int, reason: String?) {
        val requestUri = session.uriNoContextPath()
        val ctx = WsCloseContext(sessionIds[session]!!, session, statusCode, reason)

        try {
            // Invoke before handlers (if any)
            matcher.findBeforeHandlerEntries(requestUri).forEach { it.handler.closeHandler?.handleClose(ctx) }

            // Invoke endpoint handler (can't be null, otherwise the ws servlet would not have invoked the controller)
            matcher.findEndpointHandlerEntry(requestUri)!!.let { it.handler.closeHandler?.handleClose(ctx) }
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }

        // Invoke after handlers (if any)
        try {
            matcher.findAfterHandlerEntries(requestUri).forEach { it.handler.closeHandler?.handleClose(ctx) }
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }

        wsLogger?.closeHandler?.handleClose(ctx)

        // The socket has been closed, we no longer need to keep track of the session ID
        sessionIds.remove(session)
    }

    @OnWebSocketError
    fun onError(session: Session, throwable: Throwable?) {
        val requestUri = session.uriNoContextPath()
        val ctx = WsErrorContext(sessionIds[session]!!, session, throwable)

        try {
            // Invoke before handlers (if any)
            matcher.findBeforeHandlerEntries(requestUri).forEach { it.handler.errorHandler?.handleError(ctx) }

            // Invoke endpoint handler (can't be null, otherwise the ws servlet would not have invoked the controller)
            matcher.findEndpointHandlerEntry(requestUri)!!.let { it.handler.errorHandler?.handleError(ctx) }
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }

        // Invoke after handlers (if any)
        try {
            matcher.findAfterHandlerEntries(requestUri).forEach { it.handler.errorHandler?.handleError(ctx) }
        } catch (e: Exception) {
            exceptionMapper.handle(e, ctx)
        }

        wsLogger?.errorHandler?.handleError(ctx)
    }
}

private fun Session.uriNoContextPath() = this.upgradeRequest.requestURI.path.removePrefix((this.upgradeRequest as ServletUpgradeRequest).httpServletRequest.contextPath)
