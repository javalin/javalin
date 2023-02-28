/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.servlet

import io.javalin.config.JavalinConfig
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.HttpStatus.REQUEST_TIMEOUT
import io.javalin.http.servlet.SubmitOrder.FIRST
import io.javalin.http.servlet.SubmitOrder.LAST
import io.javalin.http.util.AsyncUtil.addListener
import io.javalin.http.util.AsyncUtil.isAsync
import io.javalin.http.util.ETagGenerator
import io.javalin.routing.PathMatcher
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class JavalinServlet(val cfg: JavalinConfig) : HttpServlet() {

    val matcher = PathMatcher()
    val exceptionMapper = ExceptionMapper(cfg)
    val errorMapper = ErrorMapper()

    override fun service(request: HttpServletRequest, response: HttpServletResponse) {
        try {
            val ctx = JavalinServletContext(req = request, res = response, cfg = cfg)

            val submitTask: (SubmitOrder, Task) -> Unit = { order, task ->
                when (order) {
                    FIRST -> ctx.tasks.offerFirst(task)
                    LAST -> ctx.tasks.add(task)
                }
            }
            val requestUri = ctx.path().removePrefix(ctx.contextPath())
            cfg.pvt.servletRequestLifecycle.forEach { it.createTasks(submitTask, this, ctx, requestUri) }

            ctx.handleSync()
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(response, throwable)
        }
    }

    private fun JavalinServletContext.handleSync() {
        while (userFutureSupplier == null && tasks.isNotEmpty()) {
            val task = tasks.poll()
            if (exceptionOccurred && task.skipIfExceptionOccurred) {
                continue
            }
            handleTask(task.handler)
        }
        when {
            userFutureSupplier != null -> handleUserFuture()
            else -> writeResponseAndLog()
        }
    }

    private fun JavalinServletContext.handleUserFuture() {
        val userFutureSupplier = userFutureSupplier!!.also { userFutureSupplier = null } // nullcheck in handleSync
        if (!isAsync()) startAsyncAndAddDefaultTimeoutListeners() // start async if not already started

        val userFuture = handleTask { userFutureSupplier.get() } ?: return handleSync() // get future from supplier or handle error
        req().asyncContext.addListener(onTimeout = { userFuture.cancel(true) }) // cancel user's future if timeout occurs

        userFuture
            .thenApply { handleSync() }
            .exceptionally {
                exceptionMapper.handle(this, it)
                writeResponseAndLog()
            }
    }

    private fun JavalinServletContext.startAsyncAndAddDefaultTimeoutListeners() = req().startAsync().also {
        it.timeout = cfg.http.asyncTimeout
        it.addListener(onTimeout = { // a timeout avoids the pipeline - we need to handle it manually + it's not thread-safe
            status(INTERNAL_SERVER_ERROR) // default error handling
            errorMapper.handle(statusCode(), this) // user defined error handling
            if (resultInputStream() == null) result(REQUEST_TIMEOUT.message) // write default response only if handler didn't do anything
            writeResponseAndLog()
        })
    }

    private fun <R> JavalinServletContext.handleTask(handler: TaskHandler<R>): R? =
        try {
            handler.handle()
        } catch (throwable: Throwable) {
            exceptionOccurred = true
            userFutureSupplier = null
            tasks.offerFirst(Task(skipIfExceptionOccurred = false) { exceptionMapper.handle(this, throwable) })
            null
        }

    private fun JavalinServletContext.writeResponseAndLog() {
        try {
            if (responseWritten.getAndSet(true)) return // prevent writing more than once, it's required because timeout listener can terminate the flow at any time
            resultInputStream()?.use { resultStream ->
                val etagWritten = ETagGenerator.tryWriteEtagAndClose(cfg.http.generateEtags, this, resultStream)
                if (!etagWritten) resultStream.copyTo(outputStream(), 4096)
            }
            cfg.pvt.requestLogger?.handle(this, executionTimeMs())
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(res(), throwable) // handle any unexpected error, e.g. write failure
        } finally {
            if (outputStreamWrapper.isInitialized()) outputStream().close() // close initialized output wrappers
            if (isAsync()) req().asyncContext.complete() // guarantee completion of async context to eliminate the possibility of hanging connections
        }
    }

}
