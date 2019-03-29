/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.core.util.ContextUtil
import io.javalin.json.JavalinJson
import org.eclipse.jetty.websocket.api.RemoteEndpoint
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.common.WebSocketSession
import java.nio.ByteBuffer

/**
 * The [WsContext] class holds Jetty's [Session] and provides (convenient) delegate methods.
 * It adds functionality similar to the API found in [io.javalin.Context].
 * It also adds a [send] method, which calls [RemoteEndpoint.sendString] on [Session.getRemote]
 */
abstract class WsContext(val sessionId: String, session: Session, private val pathParamMap: Map<String, String>, private val matchedPath: String) {

    private val webSocketSession = session as WebSocketSession

    fun session() = webSocketSession
    fun send(message: Any) = send(JavalinJson.toJson(message))
    fun send(message: String) = webSocketSession.remote.sendString(message)
    fun send(message: ByteBuffer) = webSocketSession.remote.sendBytes(message)
    fun queryString(): String? = webSocketSession.upgradeRequest!!.queryString
    @JvmOverloads
    fun queryParam(queryParam: String, default: String? = null): String? = queryParams(queryParam).firstOrNull()
            ?: default

    fun queryParams(queryParam: String): List<String> = queryParamMap()[queryParam] ?: emptyList()
    fun queryParamMap(): Map<String, List<String>> = ContextUtil.splitKeyValueStringAndGroupByKey(queryString() ?: "")
    fun mapQueryParams(vararg keys: String): List<String>? = ContextUtil.mapKeysOrReturnNullIfAnyNulls(keys) { queryParam(it) }
    fun anyQueryParamNull(vararg keys: String): Boolean = keys.any { queryParam(it) == null }
    fun pathParam(pathParam: String): String = ContextUtil.pathParamOrThrow(pathParamMap, pathParam, matchedPath)
    fun pathParamMap(): Map<String, String> = pathParamMap
    fun host(): String? = webSocketSession.upgradeRequest.host
    fun header(header: String): String? = webSocketSession.upgradeRequest.getHeader(header)
    fun headerMap(): Map<String, String> = webSocketSession.upgradeRequest.headers.keys.map { it to webSocketSession.upgradeRequest.getHeader(it) }.toMap()
    fun matchedPath() = matchedPath

    override fun equals(other: Any?) = webSocketSession == (other as WsContext).webSocketSession
    override fun hashCode() = webSocketSession.hashCode()
}

class WsConnectContext(sessionId: String, session: Session, pathParamMap: Map<String, String>, matchedPath: String) : WsContext(sessionId, session, pathParamMap, matchedPath)
class WsErrorContext(sessionId: String, session: Session, pathParamMap: Map<String, String>, matchedPath: String, val error: Throwable?) : WsContext(sessionId, session, pathParamMap, matchedPath)
class WsCloseContext(sessionId: String, session: Session, pathParamMap: Map<String, String>, matchedPath: String, val statusCode: Int, val reason: String?) : WsContext(sessionId, session, pathParamMap, matchedPath)
class WsBinaryMessageContext(sessionId: String, session: Session, pathParamMap: Map<String, String>, matchedPath: String, val data: Array<Byte>, val offset: Int, val length: Int) : WsContext(sessionId, session, pathParamMap, matchedPath)
class WsMessageContext(sessionId: String, session: Session, pathParamMap: Map<String, String>, matchedPath: String, private val message: String) : WsContext(sessionId, session, pathParamMap, matchedPath) {
    fun message(): String = message
    inline fun <reified T : Any> message(): T = JavalinJson.fromJson(message(), T::class.java)
}
