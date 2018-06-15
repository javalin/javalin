/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.core.PathParser
import io.javalin.core.util.Util
import org.eclipse.jetty.websocket.api.Session
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class WebSocketEntry(contextPath: String, path: String, private val wsHandler: WebSocketHandler) {

    private val pathParser: PathParser = PathParser(Util.prefixContextPath(contextPath, path))
    private val sessionPathParams = ConcurrentHashMap<Session, Map<String, String>>()
    private val sessionIds = ConcurrentHashMap<Session, String>()

    internal fun onConnect(session: Session) =
            wsHandler.connectHandler?.handle(session.wrap())

    internal fun onMessage(session: Session, message: String) =
            wsHandler.messageHandler?.handle(session.wrap(), message)

    internal fun onError(session: Session, throwable: Throwable?) =
            wsHandler.errorHandler?.handle(session.wrap(), throwable)

    internal fun onClose(session: Session, statusCode: Int, reason: String?) {
        wsHandler.closeHandler?.handle(session.wrap(), statusCode, reason)
        session.destroy()
    }

    fun matches(requestUri: String) = pathParser.matches(requestUri)

    private fun Session.wrap(): WsSession {
        sessionIds.putIfAbsent(this, UUID.randomUUID().toString())
        sessionPathParams.putIfAbsent(this, pathParser.extractPathParams(this.upgradeRequest.requestURI.path))
        return WsSession(sessionIds[this], this, sessionPathParams[this])
    }

    private fun Session.destroy() {
        sessionIds.remove(this)
        sessionPathParams.remove(this)
    }

}
