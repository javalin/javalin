/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.servlet

import io.javalin.config.JavalinConfig
import io.javalin.http.ErrorMapper
import io.javalin.http.ExceptionMapper
import io.javalin.http.HttpStatus.INTERNAL_SERVER_ERROR
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

            val submitTask: (Task) -> Unit = { ctx.tasks.add(it) }
            val requestUri = ctx.path().removePrefix(ctx.contextPath())
            cfg.pvt.servletRequestLifecycle.forEach { it.createTasks(submitTask, this, ctx, requestUri) }

            handleSync(ctx)
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(response, throwable)
        }
    }

    private fun handleSync(ctx: JavalinServletContext) {
        while (ctx.userFutureSupplier == null && ctx.tasks.isNotEmpty()) {
            val task = ctx.tasks.poll()
            if (ctx.exceptionOccurred && task.skipIfErrorOccurred) {
                continue
            }
            handleTask(ctx, task.handler)
        }

        when {
            ctx.userFutureSupplier != null -> handleAsync(ctx)
            else -> writeResponseAndLog(ctx)
        }
    }

    private fun handleAsync(ctx: JavalinServletContext) {
        val userFutureSupplier = ctx.userFutureSupplier ?: return
        ctx.userFutureSupplier = null
        val userFuture = handleTask(ctx) { userFutureSupplier.value } ?: return handleSync(ctx) // get future from supplier or handle error

        if (!ctx.isAsync()) {
            ctx.req().startAsync().addListener(onTimeout = { // a timeout avoids the pipeline - we need to handle it manually + it's not thread-safe
                ctx.status(INTERNAL_SERVER_ERROR) // default error handling
                errorMapper.handle(ctx.statusCode(), ctx) // user defined error handling
                if (ctx.resultStream() == null) ctx.result("Request timed out") // write default response only if handler didn't do anything
                writeResponseAndLog(ctx)
            }).also { asyncCtx -> asyncCtx.timeout = cfg.http.asyncTimeout }
        }

        if (ctx.isAsync()) {
            ctx.req().asyncContext.addListener(onTimeout = { userFuture.cancel(true) }) // registers timeout listener
        }

        userFuture
            .thenApply { handleSync(ctx) }
            .exceptionally {
                exceptionMapper.handle(ctx, it)
                writeResponseAndLog(ctx)
            }
    }

    private fun <R> handleTask(ctx: JavalinServletContext, handler: TaskHandler<R>): R? =
        try {
            handler.handle()
        } catch (throwable: Throwable) {
            ctx.exceptionOccurred = true
            ctx.userFutureSupplier = null
            ctx.tasks.offerFirst(Task(skipIfErrorOccurred = false) { exceptionMapper.handle(ctx, throwable) })
            null
        }

    private fun writeResponseAndLog(ctx: JavalinServletContext) {
        try {
            if (ctx.responseWritten.getAndSet(true)) return // prevent writing more than once, it's required because timeout listener can terminate the flow at any time
            ctx.outputStream().use { outputStream ->
                ctx.resultStream()?.use { resultStream ->
                    val etagWritten = ETagGenerator.tryWriteEtagAndClose(cfg.http.generateEtags, ctx, resultStream)
                    if (!etagWritten) resultStream.copyTo(outputStream, 4096)
                }
            }
            cfg.pvt.requestLogger?.handle(ctx, ctx.executionTimeMs())
        } catch (throwable: Throwable) {
            exceptionMapper.handleUnexpectedThrowable(ctx.res(), throwable) // handle any unexpected error, e.g. write failure
        } finally {
            if (ctx.isAsync()) ctx.req().asyncContext.complete() // guarantee completion of async context to eliminate the possibility of hanging connections
        }
    }

}
