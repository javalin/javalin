package io.javalin.http.util

import io.javalin.config.HttpConfig
import io.javalin.http.Context
import io.javalin.util.ConcurrencyUtil
import io.javalin.util.function.ThrowingRunnable
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException
import kotlin.Exception

internal object AsyncUtil {

    val defaultExecutor = ConcurrencyUtil.executorService("JavalinDefaultAsyncThreadPool")

    /**
     * Utility method that executes [task] asynchronously using [executor] ([defaultExecutor] by default).
     * It also provides custom timeout handling via [onTimeout] callback registered directly on underlying [CompletableFuture],
     * so global [HttpConfig.asyncTimeout] does not affect this particular task.
     */
    fun submitAsyncTask(context: Context, executor: ExecutorService?, timeout: Long, onTimeout: Runnable?, task: ThrowingRunnable<Exception>): Unit =
        context.future {
            context.req().asyncContext.timeout = 0 // we're using cf timeouts below, so we need to disable default jetty timeout listener

            CompletableFuture.runAsync({ task.run() }, executor ?: defaultExecutor)
                .let { if (timeout > 0) it.orTimeout(timeout, MILLISECONDS) else it }
                .let { if (onTimeout == null) it else it.exceptionally { exception ->
                    exception as? TimeoutException
                        ?: exception?.cause as? TimeoutException?
                        ?: throw exception // rethrow if exception or its cause is not TimeoutException
                    onTimeout.run()
                    null // handled
                }
            }
        }

    internal fun Context.isAsync(): Boolean =
        req().isAsyncStarted

    internal fun AsyncContext.addListener(
        onComplete: (AsyncEvent) -> Unit = {},
        onError: (AsyncEvent) -> Unit = {},
        onStartAsync: (AsyncEvent) -> Unit = {},
        onTimeout: (AsyncEvent) -> Unit = {},
    ): AsyncContext = apply {
        addListener(object : AsyncListener {
            override fun onComplete(event: AsyncEvent) = onComplete(event)
            override fun onError(event: AsyncEvent) = onError(event)
            override fun onStartAsync(event: AsyncEvent) = onStartAsync(event)
            override fun onTimeout(event: AsyncEvent) = onTimeout(event)
        })
    }

}
