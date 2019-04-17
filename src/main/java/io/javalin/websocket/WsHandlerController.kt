/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.core.JavalinConfig
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
class WsHandlerController(private val matcher: WsPathMatcher, private val config: JavalinConfig) {

    private val sessionIds = ConcurrentHashMap<Session, String>()

    @OnWebSocketConnect
    fun onConnect(session: Session) {
        // Associate a unique ID with this new session
        val sessionId = UUID.randomUUID().toString()
        sessionIds[session] = sessionId

        val requestUri = session.uriNoContextPath()
        val ctx = WsConnectContext(sessionId, session)

        // Invoke before handlers (if any)
        matcher.findBeforeHandlerEntries(requestUri).forEach { entry ->
            try {
                entry.handler.connectHandler?.handleConnect(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        // Invoke endpoint handler (can't be null, otherwise the ws servlet would not have invoked the controller)
        matcher.findEndpointHandlerEntry(requestUri)!!.let { entry ->
            try {
                entry.handler.connectHandler?.handleConnect(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        // Invoke after handlers (if any)
        matcher.findAfterHandlerEntries(requestUri).forEach { entry ->
            try {
                entry.handler.connectHandler?.handleConnect(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        config.inner.wsLogger?.connectHandler?.handleConnect(ctx)
    }

    @OnWebSocketMessage
    fun onMessage(session: Session, message: String) {
        val requestUri = session.uriNoContextPath()
        val ctx = WsMessageContext(sessionIds[session]!!, session, message)

        // Invoke before handlers (if any)
        matcher.findBeforeHandlerEntries(requestUri).forEach { entry ->
            try {
                entry.handler.messageHandler?.handleMessage(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        // Invoke endpoint handler (can't be null, otherwise the ws servlet would not have invoked the controller)
        matcher.findEndpointHandlerEntry(requestUri)!!.let { entry ->
            try {
                entry.handler.messageHandler?.handleMessage(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        // Invoke after handlers (if any)
        matcher.findAfterHandlerEntries(requestUri).forEach { entry ->
            try {
                entry.handler.messageHandler?.handleMessage(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        config.inner.wsLogger?.messageHandler?.handleMessage(ctx)
    }

    @OnWebSocketMessage
    fun onMessage(session: Session, buffer: ByteArray, offset: Int, length: Int) {
        val requestUri = session.uriNoContextPath()
        val ctx = WsBinaryMessageContext(sessionIds[session]!!, session, buffer.toTypedArray(), offset, length)

        // Invoke before handlers (if any)
        matcher.findBeforeHandlerEntries(requestUri).forEach { entry ->
            try {
                entry.handler.binaryMessageHandler?.handleBinaryMessage(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        // Invoke endpoint handler (can't be null, otherwise the ws servlet would not have invoked the controller)
        matcher.findEndpointHandlerEntry(requestUri)!!.let { entry ->
            try {
                entry.handler.binaryMessageHandler?.handleBinaryMessage(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        // Invoke after handlers (if any)
        matcher.findAfterHandlerEntries(requestUri).forEach { entry ->
            try {
                entry.handler.binaryMessageHandler?.handleBinaryMessage(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        config.inner.wsLogger?.binaryMessageHandler?.handleBinaryMessage(ctx)
    }

    @OnWebSocketClose
    fun onClose(session: Session, statusCode: Int, reason: String?) {
        val requestUri = session.uriNoContextPath()
        val ctx = WsCloseContext(sessionIds[session]!!, session, statusCode, reason)

        // Invoke before handlers (if any)
        matcher.findBeforeHandlerEntries(requestUri).forEach { entry ->
            try {
                entry.handler.closeHandler?.handleClose(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        // Invoke endpoint handler (can't be null, otherwise the ws servlet would not have invoked the controller)
        matcher.findEndpointHandlerEntry(requestUri)!!.let { entry ->
            try {
                entry.handler.closeHandler?.handleClose(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        // Invoke after handlers (if any)
        matcher.findAfterHandlerEntries(requestUri).forEach { entry ->
            try {
                entry.handler.closeHandler?.handleClose(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        config.inner.wsLogger?.closeHandler?.handleClose(ctx)

        // The socket has been closed, we no longer need to keep track of the session ID
        sessionIds.remove(session)
    }

    @OnWebSocketError
    fun onError(session: Session, throwable: Throwable?) {
        val requestUri = session.uriNoContextPath()
        val ctx = WsErrorContext(sessionIds[session]!!, session, throwable)

        // Invoke before handlers (if any)
        matcher.findBeforeHandlerEntries(requestUri).forEach { entry ->
            try {
                entry.handler.errorHandler?.handleError(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        // Invoke endpoint handler (can't be null, otherwise the ws servlet would not have invoked the controller)
        matcher.findEndpointHandlerEntry(requestUri)!!.let { entry ->
            try {
                entry.handler.errorHandler?.handleError(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        // Invoke after handlers (if any)
        matcher.findAfterHandlerEntries(requestUri).forEach { entry ->
            try {
                entry.handler.errorHandler?.handleError(ctx)
            } catch (e: Exception) {
                // TODO: wsExceptionMapper
            }
        }

        config.inner.wsLogger?.errorHandler?.handleError(ctx)
    }
}

private fun Session.uriNoContextPath() = this.upgradeRequest.requestURI.path.removePrefix((this.upgradeRequest as ServletUpgradeRequest).httpServletRequest.contextPath)
