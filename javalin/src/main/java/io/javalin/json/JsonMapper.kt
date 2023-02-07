/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.json

import io.javalin.Javalin
import io.javalin.http.Context
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Type
import java.util.stream.Stream
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

interface JsonMapper {
    /**
     * Javalin uses this method for [io.javalin.http.Context.json],
     * as well as the CookieStore class, WebSockets messaging, and JavalinVue.
     */
    fun toJsonString(obj: Any, type: Type): String = throw NotImplementedError("JsonMapper#toJsonString not implemented")

    /**
     * Javalin uses this method for [io.javalin.http.Context.json],
     * if called with useStreamingMapper = true.
     * When implementing this method, use (or look at) PipedStreamUtil to get
     * an InputStream from an OutputStream.
     */
    fun toJsonStream(obj: Any, type: Type): InputStream = throw NotImplementedError("JsonMapper#toJsonStream not implemented")

    /**
     * Javalin uses this method for [io.javalin.http.Context.writeJsonStream]. When implementing this method
     * you are expected to consume `stream` one element at a time, convert that element to JSON, and write that
     * element's corresponding JSON to the `outputStream`. The intent is to reduce the amount of memory used
     * during transformation of a data stream to JSON.
     *
     * It is implied that the `stream` of objects will be transformed to an Array type in JSON. In other words,
     * it is your responsibility to surround the output with `[` and `]`.
     *
     * The `outputStream` will be a compressed output stream.
     *
     * The content type header will already be set to "application/json".
     *
     * Once Javalin calls this function, it will not participate any further in the response. It becomes
     * your responsibility to deliver the remainder of the response to the `outputStream`.
     *
     * When your implementation returns from this function, assume that the underlying resources for
     * the `stream` will be released.
     */
    fun writeToOutputStream(stream: Stream<*>, outputStream: OutputStream): Unit = throw NotImplementedError("JsonMapper#writeToOutputStream not implemented")

    /**
     * If [.fromJsonStream] is not implemented, Javalin will use this method
     * when mapping request bodies to JSON through [io.javalin.http.Context.bodyAsClass].
     * Regardless of if [.fromJsonStream] is implemented, Javalin will
     * use this method for Validation and for WebSocket messaging.
     */
    fun <T : Any> fromJsonString(json: String, targetType: Type): T = throw NotImplementedError("JsonMapper#fromJsonString not implemented")

    /**
     * If implemented, Javalin will use this method instead of [.fromJsonString]
     * when mapping request bodies to JSON through [io.javalin.http.Context.bodyAsClass].
     */
    fun <T : Any> fromJsonStream(json: InputStream, targetType: Type): T {
        throw NotImplementedError("JsonMapper#fromJsonStream not implemented")
    }

}

@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> JsonMapper.toJsonString(obj: T): String = toJsonString(obj, typeOf<T>().javaType)
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> JsonMapper.toJsonStream(obj: T): InputStream = toJsonStream(obj, typeOf<T>().javaType)
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> JsonMapper.fromJsonString(json: String): T = fromJsonString(json, typeOf<T>().javaType)
@OptIn(ExperimentalStdlibApi::class)
inline fun <reified T : Any> JsonMapper.fromJsonStream(json: InputStream): T = fromJsonStream(json, typeOf<T>().javaType)

const val JSON_MAPPER_KEY = "javalin-json-mapper"
fun Javalin.jsonMapper(): JsonMapper = this.attribute(JSON_MAPPER_KEY)
fun Context.jsonMapper(): JsonMapper = this.appAttribute(JSON_MAPPER_KEY)
