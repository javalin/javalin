/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.Javalin
import io.javalin.core.util.Util

class ExceptionMapper {

    val handlers = mutableMapOf<Class<out Exception>, ExceptionHandler<Exception>?>()

    internal fun handle(exception: Exception, ctx: Context) {
        ctx.inExceptionHandler = true // prevent user from setting Future as result in exception handlers
        if (HttpResponseExceptionMapper.canHandle(exception) && noUserHandler(exception)) {
            HttpResponseExceptionMapper.handle(exception, ctx)
        } else {
            val exceptionHandler = Util.findByClass(handlers, exception.javaClass)
            if (exceptionHandler != null) {
                exceptionHandler.handle(exception, ctx)
            } else {
                Javalin.log?.warn("Uncaught exception", exception)
                HttpResponseExceptionMapper.handle(InternalServerErrorResponse(), ctx)
            }
        }
        ctx.inExceptionHandler = false
    }

    internal inline fun catchException(ctx: Context, func: () -> Unit) = try {
        func.invoke()
    } catch (e: Exception) {
        handle(e, ctx)
    }

    private fun noUserHandler(e: Exception) =
            this.handlers[e::class.java] == null && this.handlers[HttpResponseException::class.java] == null
}
