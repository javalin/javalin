/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.Javalin
import io.javalin.core.util.Util
import org.eclipse.jetty.websocket.api.StatusCode

/**
 * Maps exception types to exception handlers.
 */
class WsExceptionMapper {

    val handlers = mutableMapOf<Class<out Exception>, WsExceptionHandler<Exception>?>()

    /**
     * Handles the specific [exception]. If no handler is associated with the exception, then the
     * socket is closed with a status code that indicates internal error.
     */
    fun handle(exception: Exception, ctx: WsContext) {
        val handler = Util.findByClass(handlers, exception.javaClass)
        if (handler != null) {
            handler.handle(exception, ctx)
        } else {
            Javalin.log?.warn("Uncaught exception in WebSocket handler", exception)
            ctx.session.close(StatusCode.SERVER_ERROR, exception.message)
        }
    }

}
