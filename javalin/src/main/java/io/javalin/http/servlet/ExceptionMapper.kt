/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.servlet

import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.http.ExceptionHandler
import io.javalin.http.HttpResponseException
import io.javalin.http.HttpStatus
import io.javalin.http.InternalServerErrorResponse
import io.javalin.util.JavalinLogger
import io.javalin.util.Util
import jakarta.servlet.http.HttpServletResponse
import java.io.IOException
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

fun interface JavaLangErrorHandler {
    fun handle(res: HttpServletResponse, err: Error)
}

class ExceptionMapper(val cfg: JavalinConfig) {

    val handlers = mutableMapOf<Class<out Exception>, ExceptionHandler<Exception>?>()

    internal fun handle(ctx: Context, t: Throwable) {
        if (t is CompletionException && t.cause is Exception) {
            return handle(ctx, t.cause as Exception)
        }
        when {
            isSomewhatExpectedException(t) -> logDebugAndSetError(t, ctx.res())
            t is Exception && HttpResponseExceptionMapper.canHandle(t) && noUserHandler(t) -> HttpResponseExceptionMapper.handle(t as HttpResponseException, ctx)
            t is Exception -> Util.findByClass(handlers, t.javaClass)?.handle(t, ctx) ?: uncaughtException(ctx, t)
            else -> handleUnexpectedThrowable(ctx.res(), t)
        }
    }

    private fun uncaughtException(ctx: Context, exception: Exception) {
        JavalinLogger.warn("Uncaught exception", exception)
        HttpResponseExceptionMapper.handle(InternalServerErrorResponse(), ctx)
    }

    internal fun handleUnexpectedThrowable(res: HttpServletResponse, throwable: Throwable): Nothing? {
        res.status = HttpStatus.INTERNAL_SERVER_ERROR.code
        when {
            throwable is Error -> cfg.pvt.javaLangErrorHandler.handle(res, throwable)
            isSomewhatExpectedException(throwable) -> logDebugAndSetError(throwable, res)
            else -> JavalinLogger.error("Exception occurred while servicing http-request", throwable)
        }

        return null
    }

    private fun noUserHandler(exception: Exception) =
        this.handlers[exception::class.java] == null && this.handlers[HttpResponseException::class.java] == null

}

// Jetty throws if client aborts during response writing. testing name avoids hard dependency on jetty.
private fun isClientAbortException(t: Throwable) = t::class.java.name == "org.eclipse.jetty.io.EofException"

// Jetty may time out connections to avoid having broken connections that remain open forever
// This is rare, but intended (see issues #163 and #1277)
private fun isJettyTimeoutException(t: Throwable) = t is IOException && t.cause is TimeoutException

private fun isSomewhatExpectedException(t: Throwable): Boolean {
    val unwrapped = (t as? CompletionException)?.cause ?: t
    return isClientAbortException(unwrapped) || isJettyTimeoutException(unwrapped)
}

private fun logDebugAndSetError(t: Throwable, res: HttpServletResponse) {
    JavalinLogger.debug("Client aborted or timed out", t)
    res.status = HttpStatus.INTERNAL_SERVER_ERROR.code
}
