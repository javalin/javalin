/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.serversentevent

import io.javalin.core.PathParser
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.UpgradeRequest
import org.eclipse.jetty.websocket.api.annotations.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

data class SSEEntry(val path: String, val handler: SSEHandler) {
    private val pathParser = PathParser(path)
    fun matches(requestUri: String) = pathParser.matches(requestUri)
    fun extractPathParams(requestUri: String) = pathParser.extractPathParams(requestUri)
}

/**
 * Every WebSocket request passes through a single instance of this class.
 * Session IDs are generated and tracked here, and path-parameters are cached for performance.
 */
@WebSocket
class SSEPathMatcher() {

    val wsEntries = mutableListOf<SSEEntry>()
    private val sessionIds = ConcurrentHashMap<Session, String>()
    private val sessionPathParams = ConcurrentHashMap<Session, Map<String, String>>()


    private fun findEntry(session: Session) = findEntry(session.upgradeRequest)

    fun findEntry(req: UpgradeRequest) = wsEntries.find { it.matches(req.requestURI.path) }

//    private fun wrap(session: Session, sseEntry: SSEEntry): SSESession {
//        sessionIds.putIfAbsent(session, UUID.randomUUID().toString())
//        sessionPathParams.putIfAbsent(session, wsEntry.extractPathParams(session.upgradeRequest.requestURI.path))
//        return WsSession(sessionIds[session]!!, session, sessionPathParams[session]!!, wsEntry.path)
//    }
//
//    private fun destroy(session: Session) {
//        sessionIds.remove(session)
//        sessionPathParams.remove(session)
//    }

}
