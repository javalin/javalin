/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.core.util.ContextUtil
import org.eclipse.jetty.websocket.api.*
import org.eclipse.jetty.websocket.common.WebSocketSession
import java.net.InetSocketAddress
import java.nio.ByteBuffer

/**
 * The [WsSession] class is a wrapper for Jetty's [Session].
 * It adds functionality similar to the API found in [io.javalin.Context].
 * It also adds a [send] method, which calls [RemoteEndpoint.sendString] on [Session.getRemote]
 */
class WsSession(val id: String, session: Session, private var pathParamMap: Map<String, String>, private val matchedPath: String) : Session {

    private val webSocketSession = session as WebSocketSession

    fun send(message: String) = webSocketSession.remote.sendString(message)
    fun send(message: ByteBuffer) = webSocketSession.remote.sendBytes(message)
    fun queryString() = webSocketSession.upgradeRequest!!.queryString
    @JvmOverloads
    fun queryParam(queryParam: String, default: String? = null): String? = queryParams(queryParam).firstOrNull() ?: default

    fun queryParams(queryParam: String): List<String> = queryParamMap()[queryParam] ?: emptyList()
    fun queryParamMap(): Map<String, List<String>> = ContextUtil.splitKeyValueStringAndGroupByKey(queryString())
    fun mapQueryParams(vararg keys: String): List<String>? = ContextUtil.mapKeysOrReturnNullIfAnyNulls(keys) { queryParam(it) }
    fun anyQueryParamNull(vararg keys: String): Boolean = keys.any { queryParam(it) == null }
    fun pathParam(pathParam: String): String = ContextUtil.pathParamOrThrow(pathParamMap, pathParam, matchedPath)
    fun pathParamMap(): Map<String, String> = pathParamMap
    fun host(): String? = webSocketSession.upgradeRequest.host
    fun header(header: String): String? = webSocketSession.upgradeRequest.getHeader(header)
    fun headerMap(): Map<String, String> = webSocketSession.upgradeRequest.headers.keys.map { it to webSocketSession.upgradeRequest.getHeader(it) }.toMap()
    fun matchedPath() = matchedPath

    // interface overrides + equals/hash

    override fun close() = webSocketSession.close()
    override fun close(closeStatus: CloseStatus) = webSocketSession.close(closeStatus)
    override fun close(statusCode: Int, reason: String) = webSocketSession.close(statusCode, reason)
    override fun disconnect() = webSocketSession.disconnect()
    override fun getIdleTimeout() = webSocketSession.idleTimeout
    override fun getLocalAddress(): InetSocketAddress = webSocketSession.localAddress
    override fun getPolicy(): WebSocketPolicy = webSocketSession.policy
    override fun getProtocolVersion(): String = webSocketSession.protocolVersion
    override fun getRemote(): RemoteEndpoint = webSocketSession.remote
    override fun getRemoteAddress(): InetSocketAddress = webSocketSession.remoteAddress
    override fun getUpgradeRequest(): UpgradeRequest = webSocketSession.upgradeRequest
    override fun getUpgradeResponse(): UpgradeResponse = webSocketSession.upgradeResponse
    override fun isOpen() = webSocketSession.isOpen
    override fun isSecure() = webSocketSession.isSecure
    override fun setIdleTimeout(ms: Long) = webSocketSession.setIdleTimeout(ms)
    override fun suspend(): SuspendToken = webSocketSession.suspend()
    override fun equals(other: Any?) = webSocketSession == (other as WsSession).webSocketSession
    override fun hashCode() = webSocketSession.hashCode()
}
