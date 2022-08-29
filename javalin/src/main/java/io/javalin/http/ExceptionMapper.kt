/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http

import io.javalin.config.JavalinConfig
import io.javalin.jetty.JettyUtil
import io.javalin.util.JavalinLogger
import io.javalin.util.Util
import jakarta.servlet.http.HttpServletResponse
import java.util.concurrent.CompletionException

class SkipHttpHandlerException : RuntimeException()

class ExceptionMapper(val cfg: JavalinConfig) {

    val handlers = mutableMapOf<Class<out Exception>, ExceptionHandler<Exception>?>()

    internal fun handle(ctx: Context, throwable: Throwable) {
        if (throwable is CompletionException && throwable.cause is Exception) {
            return handle(ctx, throwable.cause as Exception)
        }

        cfg.pvt.stackTraceCleanerFunction?.run {
            throwable.stackTrace = invoke(throwable.stackTrace)
        }

        when {
            throwable is SkipHttpHandlerException -> { /* do nothing */ }
            throwable is Exception && HttpResponseExceptionMapper.canHandle(throwable) && noUserHandler(throwable) ->
                HttpResponseExceptionMapper.handle(throwable, ctx)
            throwable is Exception ->
                Util.findByClass(handlers, throwable.javaClass)
                    ?.handle(throwable, ctx)
                    ?: run { uncaughtThrowable(ctx, throwable) }
            else -> uncaughtThrowable(ctx, throwable)
        }
    }

    private fun uncaughtThrowable(ctx: Context, throwable: Throwable) {
        JavalinLogger.warn("Uncaught exception", throwable)
        HttpResponseExceptionMapper.handle(InternalServerErrorResponse(), ctx)
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
