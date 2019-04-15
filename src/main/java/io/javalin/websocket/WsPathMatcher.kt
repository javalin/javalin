/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.core.JavalinConfig
import io.javalin.core.PathParser
import io.javalin.security.Role
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.annotations.*
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class WsEntry(val path: String, val handler: WsHandler, val permittedRoles: Set<Role>) {
    private val pathParser = PathParser(path)
    fun matches(requestUri: String) = pathParser.matches(requestUri)
    fun extractPathParams(requestUri: String) = pathParser.extractPathParams(requestUri)
}

/**
 * Every WebSocket request passes through a single instance of this class.
 * Session IDs are generated and tracked here, and path-parameters are cached for performance.
 */
@WebSocket
class WsPathMatcher(val config: JavalinConfig) {

    val wsEntries = mutableListOf<WsEntry>()
    private val sessionIds = ConcurrentHashMap<Session, String>()

    @OnWebSocketConnect
    fun webSocketConnect(session: Session) {
        val entry = cacheSessionAndFindEntry(session)
        val ctx = WsConnectContext(sessionIds[session]!!, session)
        entry.handler.connectHandler?.handleConnect(ctx)
        config.wsLogger?.connectHandler?.handleConnect(ctx)
    }

    @OnWebSocketMessage
    fun webSocketMessage(session: Session, message: String) {
        val entry = cacheSessionAndFindEntry(session)
        val ctx = WsMessageContext(sessionIds[session]!!, session, message)
        entry.handler.messageHandler?.handleMessage(ctx)
        config.wsLogger?.messageHandler?.handleMessage(ctx)
    }

    @OnWebSocketMessage
    fun webSocketBinaryMessage(session: Session, buffer: ByteArray, offset: Int, length: Int) {
        val entry = cacheSessionAndFindEntry(session)
        val ctx = WsBinaryMessageContext(sessionIds[session]!!, session, buffer.toTypedArray(), offset, length)
        entry.handler.binaryMessageHandler?.handleBinaryMessage(ctx)
        config.wsLogger?.binaryMessageHandler?.handleBinaryMessage(ctx)
    }

    @OnWebSocketError
    fun webSocketError(session: Session, throwable: Throwable?) {
        val entry = cacheSessionAndFindEntry(session)
        val ctx = WsErrorContext(sessionIds[session]!!, session, throwable)
        entry.handler.errorHandler?.handleError(ctx)
        config.wsLogger?.errorHandler?.handleError(ctx)
    }

    @OnWebSocketClose
    fun webSocketClose(session: Session, statusCode: Int, reason: String?) {
        val entry = cacheSessionAndFindEntry(session)
        val ctx = WsCloseContext(sessionIds[session]!!, session, statusCode, reason)
        entry.handler.closeHandler?.handleClose(ctx)
        config.wsLogger?.closeHandler?.handleClose(ctx)
        sessionIds.remove(session)
    }

    fun findEntry(uri: String) = wsEntries.find { it.matches(uri) }

    private fun cacheSessionAndFindEntry(session: Session): WsEntry {
        sessionIds.putIfAbsent(session, UUID.randomUUID().toString())
        return findEntry(session.uriNoContextPath())!! // can't be null, has 404 handler in front
    }

}

private fun Session.uriNoContextPath() = this.upgradeRequest.requestURI.path.removePrefix((this.upgradeRequest as ServletUpgradeRequest).httpServletRequest.contextPath)
