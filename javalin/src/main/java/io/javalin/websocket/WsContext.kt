/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.http.Context
import io.javalin.jetty.upgradeContextKey
import io.javalin.jetty.upgradeSessionAttrsKey
import io.javalin.plugin.json.jsonMapper
import org.eclipse.jetty.websocket.api.RemoteEndpoint
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest
import java.nio.ByteBuffer

/**
 * The [WsContext] class holds Jetty's [Session] and provides (convenient) delegate methods.
 * It adds functionality similar to the API found in [io.javalin.Context].
 * It also adds a [send] method, which calls [RemoteEndpoint.sendString] on [Session.getRemote]
 */
abstract class WsContext(val sessionId: String, @JvmField val session: Session) {

    internal val upgradeReq by lazy { session.upgradeRequest as ServletUpgradeRequest }
    internal val upgradeCtx by lazy { upgradeReq.httpServletRequest.getAttribute(upgradeContextKey) as Context }
    internal val sessionAttributes by lazy { upgradeReq.httpServletRequest.getAttribute(upgradeSessionAttrsKey) as Map<String, Any>? }

    fun matchedPath() = upgradeCtx.matchedPath

    fun send(message: Any) = send(upgradeCtx.jsonMapper().toJsonString(message))
    fun send(message: String) = session.remote.sendStringByFuture(message)
    fun send(message: ByteBuffer) = session.remote.sendBytesByFuture(message)

    fun queryString(): String? = upgradeCtx.queryString()
    fun queryParamMap(): Map<String, List<String>> = upgradeCtx.queryParamMap()
    fun queryParams(key: String): List<String> = upgradeCtx.queryParams(key)
    fun queryParam(key: String): String? = upgradeCtx.queryParam(key)
    fun <T> queryParamAsClass(key: String, clazz: Class<T>) = upgradeCtx.queryParamAsClass(key, clazz)
    inline fun <reified T : Any> queryParamAsClass(key: String) = queryParamAsClass(key, T::class.java)

    fun pathParamMap(): Map<String, String> = upgradeCtx.pathParamMap()
    fun pathParam(key: String): String = upgradeCtx.pathParam(key)
    fun <T> pathParamAsClass(key: String, clazz: Class<T>) = upgradeCtx.pathParamAsClass(key, clazz)
    inline fun <reified T : Any> pathParamAsClass(key: String) = pathParamAsClass(key, T::class.java)

    fun host() = upgradeReq.host // why can't we get this from upgradeCtx?

    fun header(header: String): String? = upgradeCtx.header(header)
    fun headerMap(): Map<String, String> = upgradeCtx.headerMap()

    fun cookie(name: String) = upgradeCtx.cookie(name)
    fun cookieMap(): Map<String, String> = upgradeCtx.cookieMap()

    fun attribute(key: String, value: Any?) = upgradeCtx.attribute(key, value)
    fun <T> attribute(key: String): T? = upgradeCtx.attribute(key)
    fun attributeMap(): Map<String, Any?> = upgradeCtx.attributeMap()

    fun <T> sessionAttribute(key: String): T? = sessionAttributeMap()[key] as T
    fun sessionAttributeMap(): Map<String, Any?> = sessionAttributes ?: mapOf()

    override fun equals(other: Any?) = session == (other as WsContext).session
    override fun hashCode() = session.hashCode()
}

class WsConnectContext(sessionId: String, session: Session) : WsContext(sessionId, session)

class WsErrorContext(sessionId: String, session: Session, private val error: Throwable?) : WsContext(sessionId, session) {
    fun error() = error
}

class WsCloseContext(sessionId: String, session: Session, private val statusCode: Int, private val reason: String?) : WsContext(sessionId, session) {
    fun status() = statusCode
    fun reason() = reason
}

class WsBinaryMessageContext(sessionId: String, session: Session, private val data: ByteArray, private val offset: Int, private val length: Int) : WsContext(sessionId, session) {
    fun data() = data
    fun offset() = offset
    fun length() = length
}

class WsMessageContext(sessionId: String, session: Session, private val message: String) : WsContext(sessionId, session) {
    fun message(): String = message
    fun <T> messageAsClass(clazz: Class<T>): T = upgradeCtx.jsonMapper().fromJsonString(message, clazz)
    inline fun <reified T : Any> messageAsClass(): T = messageAsClass(T::class.java)
}
