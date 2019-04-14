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
import java.nio.ByteBuffer

/**
 * The [WsContext] class holds Jetty's [Session] and provides (convenient) delegate methods.
 * It adds functionality similar to the API found in [io.javalin.Context].
 * It also adds a [send] method, which calls [RemoteEndpoint.sendString] on [Session.getRemote]
 */
abstract class WsContext(val sessionId: String, @JvmField val session: Session, private val pathParamMap: Map<String, String>, private val matchedPath: String) {

    fun matchedPath() = matchedPath
    private val upgradeReq = session.upgradeRequest

    fun send(message: Any) = send(JavalinJson.toJson(message))
    fun send(message: String) = session.remote.sendStringByFuture(message)
    fun send(message: ByteBuffer) = session.remote.sendBytesByFuture(message)

    fun queryString(): String? = upgradeReq!!.queryString
    fun queryParamMap(): Map<String, List<String>> = ContextUtil.splitKeyValueStringAndGroupByKey(queryString() ?: "")
    fun queryParams(key: String): List<String> = queryParamMap()[key] ?: emptyList()
    fun queryParam(key: String): String? = queryParams(key).firstOrNull()
    fun queryParam(key: String, default: String): String? = queryParam(key) ?: default

    fun pathParamMap(): Map<String, String> = pathParamMap
    fun pathParam(pathParam: String): String = ContextUtil.pathParamOrThrow(pathParamMap, pathParam, matchedPath)

    fun host(): String? = upgradeReq.host
    fun header(header: String): String? = upgradeReq.getHeader(header)
    fun headerMap(): Map<String, String> = upgradeReq.headers.keys.map { it to upgradeReq.getHeader(it) }.toMap()

    override fun equals(other: Any?) = session == (other as WsContext).session
    override fun hashCode() = session.hashCode()
}

class WsConnectContext(sessionId: String, session: Session, pathParamMap: Map<String, String>, matchedPath: String) : WsContext(sessionId, session, pathParamMap, matchedPath)
class WsErrorContext(sessionId: String, session: Session, pathParamMap: Map<String, String>, matchedPath: String, val error: Throwable?) : WsContext(sessionId, session, pathParamMap, matchedPath)
class WsCloseContext(sessionId: String, session: Session, pathParamMap: Map<String, String>, matchedPath: String, val statusCode: Int, val reason: String?) : WsContext(sessionId, session, pathParamMap, matchedPath)
class WsBinaryMessageContext(sessionId: String, session: Session, pathParamMap: Map<String, String>, matchedPath: String, val data: Array<Byte>, val offset: Int, val length: Int) : WsContext(sessionId, session, pathParamMap, matchedPath)
class WsMessageContext(sessionId: String, session: Session, pathParamMap: Map<String, String>, matchedPath: String, private val message: String) : WsContext(sessionId, session, pathParamMap, matchedPath) {
    fun message(): String = message
    fun <T> message(clazz: Class<T>): T = JavalinJson.fromJson(message, clazz)
    inline fun <reified T : Any> message(): T = message(T::class.java)
}
