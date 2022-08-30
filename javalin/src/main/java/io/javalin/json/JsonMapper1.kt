package io.javalin.json

import java.io.InputStream
import java.lang.reflect.Type
import kotlin.NotImplementedError
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

interface JsonMapper {
    /**
     * Javalin uses this method for [io.javalin.http.Context.json],
     * as well as the CookieStore class, WebSockets messaging, and JavalinVue.
     */
    fun toJsonString(obj: Any, type: Type = obj::class.java): String {
        throw NotImplementedError("JsonMapper#toJsonString not implemented")
    }

    /**
     * Javalin uses this method for [io.javalin.http.Context.json],
     * if called with useStreamingMapper = true.
     * When implementing this method, use (or look at) PipedStreamUtil to get
     * an InputStream from an OutputStream.
     */
    fun toJsonStream(obj: Any, type: Type = obj::class.java): InputStream {
        throw NotImplementedError("JsonMapper#toJsonStream not implemented")
    }

    /**
     * If [.fromJsonStream] is not implemented, Javalin will use this method
     * when mapping request bodies to JSON through [io.javalin.http.Context.bodyAsClass].
     * Regardless of if [.fromJsonStream] is implemented, Javalin will
     * use this method for Validation and for WebSocket messaging.
     */
    fun <T : Any> fromJsonString(json: String, targetType: Type): T {
        throw NotImplementedError("JsonMapper#fromJsonString not implemented")
    }

    /**
     * If implemented, Javalin will use this method instead of [.fromJsonString]
     * when mapping request bodies to JSON through [io.javalin.http.Context.bodyAsClass].
     */
    fun <T : Any> fromJsonStream(json: InputStream, targetType: Type): T {
        throw NotImplementedError("JsonMapper#fromJsonStream not implemented")
    }

}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> JsonMapper.toJsonString(obj: T): String =
    toJsonString(obj, typeOf<T>().javaType)

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> JsonMapper.fromJsonString(json: String): T {
    return fromJsonString(json, typeOf<T>().javaType)
}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> JsonMapper.fromJsonStream(json: InputStream): T {
    return fromJsonStream(json, typeOf<T>().javaType)
}
