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

    internal fun handle(exception: Exception, ctx: Context) {
        if (exception is CompletionException && exception.cause is java.lang.Exception) {
            return handle(exception.cause as java.lang.Exception, ctx)
        }
        cfg.pvt.stackTraceCleanerFunction?.let { exception.stackTrace = it.invoke(exception.stackTrace) }
        if (exception is SkipHttpHandlerException) {
            // do nothing
        } else if (HttpResponseExceptionMapper.canHandle(exception) && noUserHandler(exception)) {
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

    internal fun handleUnexpectedThrowable(res: HttpServletResponse, throwable: Throwable) {
        val unwrapped = (throwable as? CompletionException)?.cause ?: throwable
        if (JettyUtil.isClientAbortException(unwrapped) || JettyUtil.isJettyTimeoutException(unwrapped)) {
            JavalinLogger.debug("Client aborted or timed out", throwable)
        }
        res.status = HttpStatus.INTERNAL_SERVER_ERROR.code
        // this might happen while writing the response, so we don't want to try writing anything
        JavalinLogger.error("Exception occurred while servicing http-request", throwable)
    }

    private fun noUserHandler(e: Exception) =
        this.handlers[e::class.java] == null && this.handlers[HttpResponseException::class.java] == null
}
