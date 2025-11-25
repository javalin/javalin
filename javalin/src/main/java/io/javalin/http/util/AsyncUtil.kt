package io.javalin.http.util

import io.javalin.config.HttpConfig
import io.javalin.config.Key
import io.javalin.http.Context
import io.javalin.util.function.ThrowingRunnable
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

class AsyncTaskConfig {
    /**
     * Thread-pool used to execute the given task,
     * You can change this default in [io.javalin.config.JavalinState].
     */
    @JvmField var executor: ExecutorService? = null
    /**
     * Timeout in milliseconds,
     * by default it's 0 which means timeout watcher is disabled.
     */
    @JvmField var timeout: Long = 0

    @JvmSynthetic internal var onTimeout: Consumer<Context>? = null

    /**
     * Timeout listener executed when [TimeoutException] is thrown in specified task.
     * This timeout listener is a part of request lifecycle, so you can still modify context here.
     */
    fun onTimeout(timeoutListener: Consumer<Context>) {
        this.onTimeout = timeoutListener
    }

}

class AsyncExecutor(private val defaultExecutor: ExecutorService) {

    companion object {
        @JvmStatic val AsyncExecutorKey = Key<AsyncExecutor>("javalin-default-async-executor")
    }

    /**
     * Utility method that executes [task] asynchronously using [executor] ([defaultExecutor] by default).
     * It also provides custom timeout handling via [onTimeout] callback registered directly on underlying [CompletableFuture],
     * so global [HttpConfig.asyncTimeout] does not affect this particular task.
     */
    fun submitAsyncTask(context: Context, asyncTaskConfig: AsyncTaskConfig, task: ThrowingRunnable<Exception>): Unit =
        context.future {
            context.req().asyncContext.timeout = 0 // we're using cf timeouts below, so we need to disable default jetty timeout listener

            CompletableFuture.runAsync({ task.run() }, asyncTaskConfig.executor ?: defaultExecutor)
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

}

object AsyncUtil {

    internal fun Context.isAsync(): Boolean =
        req().isAsyncStarted

    @JvmOverloads
    fun newAsyncListener(
        onStartAsync: (AsyncEvent) -> Unit = {},
        onError: (AsyncEvent) -> Unit = {},
        onTimeout: (AsyncEvent) -> Unit = {},
        onComplete: (AsyncEvent) -> Unit = {},
    ): AsyncListener =
        object : AsyncListener {
            override fun onComplete(event: AsyncEvent) = onComplete(event)
            override fun onError(event: AsyncEvent) = onError(event)
            override fun onStartAsync(event: AsyncEvent) = onStartAsync(event)
            override fun onTimeout(event: AsyncEvent) = onTimeout(event)
        }

}
