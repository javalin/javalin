/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.websocket

enum class WsHandlerType { WEBSOCKET_BEFORE, WEBSOCKET, WEBSOCKET_AFTER }

fun interface WsConnectHandler {
    @Throws(Exception::class)
    fun handleConnect(ctx: WsConnectContext)
}

fun interface WsMessageHandler {
    @Throws(Exception::class)
    fun handleMessage(ctx: WsMessageContext)
}

fun interface WsBinaryMessageHandler {
    @Throws(Exception::class)
    fun handleBinaryMessage(ctx: WsBinaryMessageContext)
}

fun interface WsPingHandler {
    @Throws(Exception::class)
    fun handlePing(ctx: WsPingContext)
}

fun interface WsPongHandler {
    @Throws(Exception::class)
    fun handlePong(ctx: WsPongContext)
}

fun interface WsErrorHandler {
    @Throws(Exception::class)
    fun handleError(ctx: WsErrorContext)
}

fun interface WsCloseHandler {
    @Throws(Exception::class)
    fun handleClose(ctx: WsCloseContext)
}
