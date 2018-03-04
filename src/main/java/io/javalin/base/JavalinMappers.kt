package io.javalin.base

import io.javalin.ErrorHandler
import io.javalin.ExceptionHandler
import io.javalin.Javalin
import io.javalin.core.ErrorMapper
import io.javalin.core.ExceptionMapper

internal abstract class JavalinMappers : JavalinBase() {

    protected val exceptionMapper = ExceptionMapper()
    protected val errorMapper = ErrorMapper()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Exception> exception(clazz: Class<T>, handler: ExceptionHandler<in T>): Javalin {
        exceptionMapper.exceptionMap[clazz] = handler as ExceptionHandler<Exception>
        return this
    }

    override fun error(statusCode: Int, handler: ErrorHandler): Javalin {
        errorMapper.errorHandlerMap[statusCode] = handler
        return this
    }
}