package io.javalin.http.util

import io.javalin.config.HttpConfig
import io.javalin.http.Context
import io.javalin.util.function.ThrowingRunnable
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

class AsyncTaskConfig(
    /**
     * Thread-pool used to execute the given task,
     * You can change this default in [io.javalin.config.JavalinConfig].
     */
    @JvmField var executor: ExecutorService? = null,
    /**
     * Timeout in milliseconds,
     * by default it's 0 which means timeout watcher is disabled.
     */
    @JvmField var timeout: Long = 0,
    /**
     * Timeout listener executed when [TimeoutException] is thrown in specified task.
     * This timeout listener is a part of request lifecycle, so you can still modify context here.
     */
    @JvmField var onTimeout: Consumer<Context>? = null,
)

internal object AsyncUtil {

    /**
     * Utility method that executes [task] asynchronously using [asyncTaskConfig.executor] ([defaultExecutor] by default).
     * It also provides custom timeout handling via [onTimeout] callback registered directly on underlying [CompletableFuture],
     * so global [HttpConfig.asyncTimeout] does not affect this particular task.
     */
    fun submitAsyncTask(context: Context, asyncTaskConfig: AsyncTaskConfig, task: ThrowingRunnable<Exception>): Unit =
        context.future {
            context.req().asyncContext.timeout = 0 // we're using cf timeouts below, so we need to disable default jetty timeout listener

            CompletableFuture.runAsync({ task.run() }, asyncTaskConfig.executor)
                .let { taskFuture ->
                    asyncTaskConfig.timeout
                        .takeIf { it > 0 }
                        ?.let { taskFuture.orTimeout(it, MILLISECONDS) }
                        ?: taskFuture
                }
                .let { taskFuture ->
                    asyncTaskConfig.onTimeout
                        ?.let {
                            taskFuture.exceptionally { exception ->
                                exception as? TimeoutException
                                    ?: exception?.cause as? TimeoutException?
                                    ?: throw exception // rethrow if exception or its cause is not TimeoutException
                                it.accept(context)
                                null // handled
                            }
                        }
                        ?: taskFuture
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
