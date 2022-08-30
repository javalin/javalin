/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.servlet

import io.javalin.config.JavalinConfig
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
import io.javalin.http.util.AsyncUtil.addListener
import io.javalin.http.util.AsyncUtil.isAsync
import io.javalin.http.util.ETagGenerator
import io.javalin.routing.PathMatcher
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.util.concurrent.CompletableFuture

class JavalinServlet(val cfg: JavalinConfig) : HttpServlet() {

    val matcher = PathMatcher()
    val exceptionMapper = ExceptionMapper(cfg)
    val errorMapper = ErrorMapper()

    override fun service(request: HttpServletRequest, response: HttpServletResponse) {
        try {
            val ctx = JavalinServletContext(req = request, res = response, cfg = cfg)

            val submitTask: (Task) -> Unit = { ctx.tasks.add(it) }
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
            if (exceptionOccurred && task.skipIfErrorOccurred) {
                continue
            }
            handleTask(task.handler)
        }
        when {
            userFutureSupplier != null -> handleAsync()
            else -> writeResponseAndLog()
        }
    }

    private fun JavalinServletContext.handleAsync() {
        val userFutureSupplier = userFutureSupplier ?: return
        this.userFutureSupplier = null
        val userFuture = handleTask { userFutureSupplier.value } ?: return handleSync() // get future from supplier or handle error

        handleAsyncContextAndAddDefaultTimeoutListeners(userFuture)
            .thenApply { handleSync() }
            .exceptionally {
                exceptionMapper.handle(this, it)
                writeResponseAndLog()
            }
    }

    private fun JavalinServletContext.handleAsyncContextAndAddDefaultTimeoutListeners(userFuture: CompletableFuture<*>) = userFuture.also {
        if (!isAsync()) {
            req().startAsync().also {
                it.addListener(onTimeout = { // a timeout avoids the pipeline - we need to handle it manually + it's not thread-safe
                    status(INTERNAL_SERVER_ERROR) // default error handling
                    errorMapper.handle(statusCode(), this) // user defined error handling
                    if (resultStream() == null) result("Request timed out") // write default response only if handler didn't do anything
                    writeResponseAndLog()
                })
                it.timeout = cfg.http.asyncTimeout
            }
        }
        if (isAsync()) {
            req().asyncContext.addListener(onTimeout = { userFuture.cancel(true) }) // registers timeout listener
        }
    }

    private fun <R> JavalinServletContext.handleTask(handler: TaskHandler<R>): R? =
        try {
            handler.handle()
        } catch (throwable: Throwable) {
            this.exceptionOccurred = true
            this.userFutureSupplier = null
            this.tasks.offerFirst(Task(skipIfErrorOccurred = false) { exceptionMapper.handle(this, throwable) })
            null
        }

    private fun JavalinServletContext.writeResponseAndLog() {
        try {
            if (responseWritten.getAndSet(true)) return // prevent writing more than once, it's required because timeout listener can terminate the flow at any time
            outputStream().use { outputStream ->
                resultStream()?.use { resultStream ->
                    val etagWritten = ETagGenerator.tryWriteEtagAndClose(cfg.http.generateEtags, this, resultStream)
                    if (!etagWritten) resultStream.copyTo(outputStream, 4096)
                }
            }
            cfg.pvt.requestLogger?.handle(this, executionTimeMs())
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(res(), throwable) // handle any unexpected error, e.g. write failure
        } finally {
            if (isAsync()) req().asyncContext.complete() // guarantee completion of async context to eliminate the possibility of hanging connections
        }
    }

}
