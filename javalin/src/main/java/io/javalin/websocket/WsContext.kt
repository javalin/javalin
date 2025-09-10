/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.http.Context
import io.javalin.http.servlet.JavalinWsServletContext
import io.javalin.validation.Validation
import org.eclipse.jetty.websocket.api.Callback
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.core.CloseStatus
import java.lang.reflect.Type
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

/**
 * The [WsContext] class holds Jetty's [Session] and provides (convenient) delegate methods.
 * It adds functionality similar to the API found in [io.javalin.http.Context].
 * It also adds a [send] method, which calls [RemoteEndpoint.sendString] on [Session.getRemote]
 */
abstract class WsContext(val upgradeCtx: JavalinWsServletContext) {

    @JvmField val session: Session = upgradeCtx.extractedData.session
    @JvmField val sessionId: String = upgradeCtx.extractedData.sessionId

    /** Returns the path that was used to match this request */
    fun matchedPath() = upgradeCtx.matchedPath()

    /** Reified version of [sendAsClass] (Kotlin only) */
    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified T : Any> sendAsClass(message: T) = sendAsClass(message, typeOf<T>().javaType)

    /** Serializes object to a JSON-string using the registered [io.javalin.json.JsonMapper] and sends it over the socket */
    fun send(message: Any) = sendAsClass(message, message::class.java)

    /** Serializes object to a JSON-string using the registered [io.javalin.json.JsonMapper] and sends it over the socket */
    fun sendAsClass(message: Any, type: Type) = send(upgradeCtx.jsonMapper().toJsonString(message, type))

    /** Sends a [String] over the socket */
    fun send(message: String) = session.sendText(message, Callback.NOOP)

    /** Sends a [ByteBuffer] over the socket */
    fun send(message: ByteBuffer) = session.sendBinary(message, Callback.NOOP)

    /** Sends a ping over the socket */
    @JvmOverloads
    fun sendPing(applicationData: ByteBuffer? = null) = session.sendPing(applicationData ?: ByteBuffer.allocate(0), Callback.NOOP)

    /** Enables automatic pings at a 15-second interval, preventing the connection from timing out */
    fun enableAutomaticPings() {
        enableAutomaticPings(15, TimeUnit.SECONDS, null)
    }

    /** Enables automatic pings at the specified interval, preventing the connection from timing out */
    @JvmOverloads
    fun enableAutomaticPings(interval: Long, unit: TimeUnit, applicationData: ByteBuffer? = null) {
        enableAutomaticPings(this, interval, unit, applicationData)
    }

    /** Disables automatic pings */
    fun disableAutomaticPings() {
        disableAutomaticPings(this)
    }

    /** Returns the full query [String], or null if no query is present */
    fun queryString(): String? = upgradeCtx.queryString()

    /** Returns a [Map] of all the query parameters */
    fun queryParamMap(): Map<String, List<String>> = upgradeCtx.extractedData.queryParamMap

    /** Returns a [List] of all the query parameters for the given key, or an empty [List] if no such parameter exists */
    fun queryParams(key: String): List<String> = queryParamMap()[key] ?: emptyList()

    /** Returns the first query parameter for the given key, or null if no such parameter exists */
    fun queryParam(key: String): String? = queryParams(key).firstOrNull()

    /** Creates a typed [io.javalin.validation.Validator] for the [queryParam] value */
    fun <T> queryParamAsClass(key: String, clazz: Class<T>) = upgradeCtx.appData(Validation.ValidationKey).validator(key, clazz, queryParam(key))

    /** Reified version of [queryParamAsClass] (Kotlin only) */
    inline fun <reified T : Any> queryParamAsClass(key: String) = queryParamAsClass(key, T::class.java)

    /** Returns a [Map] of all the path parameters */
    fun pathParamMap(): Map<String, String> = upgradeCtx.pathParamMap()

    /** Returns a [String] of the session id */
    fun sessionId(): String = sessionId

    /** Returns a path param by name (ex: pathParam("param")).
     *
     * Ex: If the handler path is /users/{user-id}, and a browser GETs /users/123, pathParam("user-id") will return "123"
     */
    fun pathParam(key: String): String = upgradeCtx.pathParam(key)

    /** Creates a typed [io.javalin.validation.Validator] for the [pathParam] value */
    fun <T> pathParamAsClass(key: String, clazz: Class<T>) = upgradeCtx.pathParamAsClass(key, clazz)

    /** Reified version of [pathParamAsClass] (Kotlin only) */
    inline fun <reified T : Any> pathParamAsClass(key: String) = pathParamAsClass(key, T::class.java)

    /** Returns the host as a [String] */
    fun host(): String? = upgradeCtx.extractedData.host

    /** Gets a request header by name, or null. */
    fun header(header: String): String? = upgradeCtx.extractedData.headerMap[header]

    /** Gets a [Map] with all the header keys and values  */
    fun headerMap(): Map<String, String> = upgradeCtx.extractedData.headerMap

    /** Gets a request cookie by name, or null. */
    fun cookie(name: String) = cookieMap()[name]

    /** Gets a [Map] with all the request cookies */
    fun cookieMap(): Map<String, String> = upgradeCtx.extractedData.cookieMap

    /** Sets an attribute on the request. Attributes are available to other handlers in the request lifecycle. */
    fun attribute(key: String, value: Any?) = upgradeCtx.extractedData.attributeMap.put(key, value)

    /** Gets the specified attribute from the request. */
    fun <T> attribute(key: String): T? = attributeMap()[key] as T?

    /** Gets a [Map] with all the attribute keys and values on the request */
    fun attributeMap(): Map<String, Any?> = upgradeCtx.extractedData.attributeMap

    /** Gets a session attribute by name */
    @Suppress("UNCHECKED_CAST")
    fun <T> sessionAttribute(key: String): T? = sessionAttributeMap()[key] as T

    /** Gets a [Map] with all the session attributes */
    fun sessionAttributeMap(): Map<String, Any?> = upgradeCtx.extractedData.sessionAttributeMap

    /** Close the session */
    fun closeSession(): Unit = session.close()

    /** Close the session with a [CloseStatus] */
    fun closeSession(closeStatus: CloseStatus): Unit = closeSession(closeStatus.code, closeStatus.reason)

    /** Close the session with a code and reason */
    fun closeSession(code: Int, reason: String?): Unit = session.close(code, reason, Callback.NOOP)

    /** Close the session with a [WsCloseStatus] */
    @JvmOverloads
    fun closeSession(closeStatus: WsCloseStatus, reason: String? = null): Unit = closeSession(closeStatus.code, reason ?: closeStatus.message())

    override fun equals(other: Any?): Boolean = session == (other as WsContext).session
    override fun hashCode(): Int = session.hashCode()
}

class WsConnectContext(upgradeCtx: JavalinWsServletContext) : WsContext(upgradeCtx)

class WsErrorContext(upgradeCtx: JavalinWsServletContext, private val error: Throwable?) : WsContext(upgradeCtx) {
    /** Get the [Throwable] error that occurred */
    fun error(): Throwable? = error
}

class WsCloseContext(upgradeCtx: JavalinWsServletContext, private val statusCode: Int, private val reason: String?) : WsContext(upgradeCtx) {
    /** The int status for why connection was closed */
    fun status(): Int = statusCode

    /** The enum status for why connection was closed */
    fun closeStatus(): WsCloseStatus = WsCloseStatus.forStatusCode(statusCode)

    /** The reason for the close */
    fun reason(): String? = reason
}

class WsBinaryMessageContext(upgradeCtx: JavalinWsServletContext, private val data: ByteBuffer) : WsContext(upgradeCtx) {
    /** Get the binary data of the message */
    fun data(): ByteBuffer = data
}

class WsMessageContext(upgradeCtx: JavalinWsServletContext, private val message: String) : WsContext(upgradeCtx) {
    /** Receive a string message from the client */
    fun message(): String = message

    /** Receive a message from the client as a class */
    fun <T> messageAsClass(type: Type): T = upgradeCtx.jsonMapper().fromJsonString(message, type)

    /** See Also: [messageAsClass] */
    fun <T> messageAsClass(clazz: Class<T>): T = messageAsClass(type = clazz as Type)

    /** Reified version of [messageAsClass] (Kotlin only) */
    @OptIn(ExperimentalStdlibApi::class)
    inline fun <reified T : Any> messageAsClass(): T = messageAsClass(typeOf<T>().javaType)
}
