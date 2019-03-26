/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.core.util.ContextUtil
import org.eclipse.jetty.websocket.api.*
import org.eclipse.jetty.websocket.common.WebSocketSession
import java.nio.ByteBuffer

/**
 * The [WsContext] class holds Jetty's [Session] and provides (convenient) delegate methods.
 * It adds functionality similar to the API found in [io.javalin.Context].
 * It also adds a [send] method, which calls [RemoteEndpoint.sendString] on [Session.getRemote]
 */
class WsContext(val id: String, session: Session, private var pathParamMap: Map<String, String>, private val matchedPath: String) {

    private val webSocketSession = session as WebSocketSession

    fun session() = webSocketSession
    fun send(message: String) = webSocketSession.remote.sendString(message)
    fun send(message: ByteBuffer) = webSocketSession.remote.sendBytes(message)
    fun queryString(): String? = webSocketSession.upgradeRequest!!.queryString
    @JvmOverloads
    fun queryParam(queryParam: String, default: String? = null): String? = queryParams(queryParam).firstOrNull() ?: default

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
