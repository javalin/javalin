package io.javalin.websocket.context

import io.javalin.websocket.WsContext
import io.javalin.websocket.WsMessageContext

@JvmSynthetic
inline fun <reified T : Any> WsContext.queryParam(key: String, default: String? = null) = queryParam(key, T::class.java, default)

@JvmSynthetic
inline fun <reified T : Any> WsContext.pathParam(key: String) = pathParam(key, T::class.java)

@JvmSynthetic
inline fun <reified T : Any> WsMessageContext.message(): T = message(T::class.java)
