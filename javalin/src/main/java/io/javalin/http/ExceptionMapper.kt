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
import jakarta.servlet.http.HttpServletResponse

class ExceptionMapper {

    val handlers = mutableMapOf<Class<out Exception>, ExceptionHandler<Exception>?>()

    internal fun handle(exception: Exception, ctx: Context) {
        if (HttpResponseExceptionMapper.canHandle(exception) && noUserHandler(exception)) {
            HttpResponseExceptionMapper.handle(exception, ctx)
        } else {
            val exceptionHandler = Util.findByClass(handlers, exception.javaClass)
            if (exceptionHandler != null) {
                exceptionHandler.handle(exception, ctx)
            } else {
                JavalinLogger.warn("Uncaught exception", exception)
                HttpResponseExceptionMapper.handle(InternalServerErrorResponse(), ctx)
            }
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
        val unwrapped = (throwable as? CompletionException)?.cause ?: throwable
        if (JettyUtil.isClientAbortException(unwrapped) || JettyUtil.isJettyTimeoutException(unwrapped)) {
            JavalinLogger.debug("Client aborted or timed out", throwable)
            return null // jetty aborts and timeouts happen when clients disconnect, they are not actually unexpected
        }
        res.status = 500
        JavalinLogger.error("Exception occurred while servicing http-request", throwable)
        return null
    }

    private fun noUserHandler(e: Exception) =
        this.handlers[e::class.java] == null && this.handlers[HttpResponseException::class.java] == null
}
