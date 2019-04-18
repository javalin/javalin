/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket

import io.javalin.Javalin
import org.eclipse.jetty.websocket.api.StatusCode
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.full.superclasses

/**
 * Maps exception types to exception handlers.
 */
class WsExceptionMapper {

    private val handlers = ConcurrentHashMap<Class<out Exception>, WsExceptionHandler<Exception>?>()

    /** Associates an [exceptionHandler] with the specific [exceptionClass]. */
    fun addHandler(exceptionClass: Class<out Exception>, exceptionHandler: WsExceptionHandler<Exception>?) =
            handlers.put(exceptionClass, exceptionHandler)

    /**
     * Handles the specific [exception]. If no handler is associated with the exception, then the
     * socket is closed with a status code that indicates internal error.
     */
    fun handle(exception: Exception, ctx: WsContext) {
        val handler: WsExceptionHandler<Exception>? = findHandler(exception.javaClass)
        if (handler != null) {
            handler.handle(exception, ctx)
        } else {
            Javalin.log.warn("Uncaught exception in WebSocket handler", exception)
            ctx.session.close(StatusCode.SERVER_ERROR, exception.message)
        }
    }

    private fun findHandler(exceptionClass: Class<out Exception>): WsExceptionHandler<Exception>? {
        return handlers.getOrElse(exceptionClass) {
            exceptionClass.kotlin.superclasses
                    .firstOrNull { superclass -> handlers.containsKey(superclass.java) }
                    ?.let { superclass -> handlers[superclass.java] }
        }
    }
}
