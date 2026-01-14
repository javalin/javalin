/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.router.exception

import io.javalin.config.JettyConfig
import io.javalin.config.RouterConfig
import io.javalin.http.Context
import io.javalin.http.ExceptionHandler
import io.javalin.http.HttpResponseException
import io.javalin.http.HttpStatus
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.InternalServerErrorResponse
import io.javalin.util.JavalinLogger
import io.javalin.util.Util
import jakarta.servlet.http.HttpServletResponse
import java.io.IOException
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

class ExceptionMapper(private val routerConfig: RouterConfig, private val jettyConfig: JettyConfig) {

    val handlers = mutableMapOf<Class<out Exception>, ExceptionHandler<Exception>?>()

    init {
        handlers[HttpResponseException::class.java] = ExceptionHandler { e, ctx ->
            HttpResponseExceptionMapper.handle(e as HttpResponseException, ctx)
        }
    }

    internal fun handle(ctx: Context, t: Throwable) {
        if (t is CompletionException && t.cause is Exception) {
            return handle(ctx, t.cause as Exception)
        }
        when {
            isClientAbortException(t) -> logDebugAndSetError(t, ctx.res(), HttpStatus.forStatus(jettyConfig.clientAbortStatus))
            isJettyTimeoutException(t) -> logDebugAndSetError(t, ctx.res(), HttpStatus.forStatus(jettyConfig.timeoutStatus))
            t is Exception -> handleExceptionSafely(ctx, t)
            else -> handleUnexpectedThrowable(ctx.res(), t)
        }
    }

    private fun handleExceptionSafely(ctx: Context, exception: Exception) {
        try {
            Util.findByClass(handlers, exception.javaClass)?.handle(exception, ctx) ?: uncaughtException(ctx, exception)
        } catch (exceptionInHandler: Throwable) {
            JavalinLogger.warn("Exception handler for %s threw an error".format(exception.javaClass.simpleName), exceptionInHandler)
            uncaughtException(ctx, exception)
        }
    }

    private fun uncaughtException(ctx: Context, exception: Exception) {
        JavalinLogger.warn("Uncaught exception", exception)
        HttpResponseExceptionMapper.handle(InternalServerErrorResponse(), ctx)
    }

    internal fun handleUnexpectedThrowable(res: HttpServletResponse, throwable: Throwable): Nothing? {
        res.status = INTERNAL_SERVER_ERROR.code
        when {
            throwable is Error -> routerConfig.javaLangErrorHandler.handle(res, throwable)
            isClientAbortException(throwable) -> logDebugAndSetError(throwable, res, HttpStatus.forStatus(jettyConfig.clientAbortStatus))
            isJettyTimeoutException(throwable) -> logDebugAndSetError(throwable, res, HttpStatus.forStatus(jettyConfig.timeoutStatus))
            else -> JavalinLogger.error("Exception occurred while servicing http-request", throwable)
        }
        return null
    }

}

private fun unwrap(t: Throwable) = (t as? CompletionException)?.cause ?: t

// Jetty throws if client aborts during response writing. testing name avoids hard dependency on jetty.
internal fun isClientAbortException(t: Throwable) = unwrap(t)::class.java.name == "org.eclipse.jetty.io.EofException"

// Jetty may time out connections to avoid having broken connections that remain open forever
// This is rare, but intended (see issues #163 and #1277)
private fun isJettyTimeoutException(t: Throwable) = unwrap(t) is IOException && t.cause is TimeoutException

private fun logDebugAndSetError(t: Throwable, res: HttpServletResponse, status: HttpStatus) {
    JavalinLogger.debug("HTTP " + status.code + " " + status.message, t)
    res.status = status.code
}
