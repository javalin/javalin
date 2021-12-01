/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.core.util.JavalinLogger
import io.javalin.core.util.Util
import io.javalin.jetty.JettyUtil
import java.util.concurrent.CompletionException
import javax.servlet.http.HttpServletResponse

class ExceptionMapper {

    val handlers = mutableMapOf<Class<out Exception>, ExceptionHandler<Exception>?>()

    internal fun handle(throwable: Throwable, ctx: Context) {
        if (throwable is Exception) {
            ctx.inExceptionHandler = true // prevent user from setting Future as result in exception handlers
            if (HttpResponseExceptionMapper.canHandle(throwable) && noUserHandler(throwable)) {
                HttpResponseExceptionMapper.handle(throwable, ctx)
            } else {
                val exceptionHandler = Util.findByClass(handlers, throwable.javaClass)
                if (exceptionHandler != null) {
                    exceptionHandler.handle(throwable, ctx)
                } else {
                    JavalinLogger.warn("Uncaught exception", throwable)
                    HttpResponseExceptionMapper.handle(InternalServerErrorResponse(), ctx)
                }
            }
            ctx.inExceptionHandler = false
        }
        else {
            JavalinLogger.warn("Uncaught exception", throwable)
            HttpResponseExceptionMapper.handle(InternalServerErrorResponse(), ctx)
        }
    }

    internal fun handleFutureException(ctx: Context, throwable: Throwable): Nothing? {
        if (throwable is CompletionException && throwable.cause is Exception) {
            handle(throwable.cause as Exception, ctx)
        } else if (throwable is Exception) {
            handle(throwable, ctx)
        }
        return null
    }

    internal fun handleUnexpectedThrowable(res: HttpServletResponse, throwable: Throwable): Nothing? {
        if (JettyUtil.isClientAbortException(throwable)) return null // jetty aborts aren't actually unexpected
        if (JettyUtil.isJettyTimeoutException(throwable)) return null // jetty timeouts aren't actually unexpected
        res.status = 500
        JavalinLogger.error("Exception occurred while servicing http-request", throwable)
        return null
    }

    private fun noUserHandler(e: Exception) =
        this.handlers[e::class.java] == null && this.handlers[HttpResponseException::class.java] == null
}
