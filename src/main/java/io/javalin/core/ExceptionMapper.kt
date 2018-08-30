/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core

import io.javalin.Context
import io.javalin.ExceptionHandler
import io.javalin.core.util.HttpResponseExceptionMapper
import org.slf4j.LoggerFactory
import java.util.*
import javax.servlet.http.HttpServletResponse

class ExceptionMapper {

    private val log = LoggerFactory.getLogger(ExceptionMapper::class.java)

    val exceptionMap = HashMap<Class<out Exception>, ExceptionHandler<Exception>?>()

    internal fun handle(exception: Exception, ctx: Context) {
        if (HttpResponseExceptionMapper.shouldHandleException(exception)) {
            return HttpResponseExceptionMapper.handleException(exception, ctx)
        }
        ctx.inExceptionHandler = true // prevent user from setting Future as result in exception handlers
        val exceptionHandler = this.getHandler(exception.javaClass)
        if (exceptionHandler != null) {
            exceptionHandler.handle(exception, ctx)
        } else {
            log.warn("Uncaught exception", exception)
            ctx.result("Internal server error")
            ctx.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
        }
        ctx.inExceptionHandler = false
    }

    internal inline fun catchException(ctx: Context, func: () -> Unit) = try {
        func.invoke()
    } catch (e: Exception) {
        handle(e, ctx)
    }

    private fun getHandler(exceptionClass: Class<out Exception>): ExceptionHandler<Exception>? {
        if (this.exceptionMap.containsKey(exceptionClass)) {
            return this.exceptionMap[exceptionClass]
        }
        var superclass: Class<*>? = exceptionClass.superclass
        while (superclass != null) {
            if (this.exceptionMap.containsKey(superclass)) {
                val matchingHandler = this.exceptionMap[superclass]
                this.exceptionMap[exceptionClass] = matchingHandler // superclass was found, avoid search next time
                return matchingHandler
            }
            superclass = superclass.superclass
        }
        this.exceptionMap[exceptionClass] = null // nothing was found, avoid search next time
        return null
    }

}
