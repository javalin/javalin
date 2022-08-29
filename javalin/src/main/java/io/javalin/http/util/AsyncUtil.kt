package io.javalin.http.util

import io.javalin.http.Context
import io.javalin.util.exceptionallyAccept
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException

object AsyncUtil {

    /** Defines default [ExecutorService] used by [Context.future] */
    const val ASYNC_EXECUTOR_KEY = "javalin-context-async-executor"

    fun submitAsyncTask(context: Context, executor: ExecutorService, onSuccess: (() -> Unit)?, timeout: Long, onTimeout: (() -> Unit)?, task: Runnable): Context =
        context.future {
            CompletableFuture.runAsync(task, executor)
                .let { if (timeout > 0) it.orTimeout(timeout, MILLISECONDS) else it }
                .let { if (onSuccess != null) it.thenAccept { onSuccess() } else it }
                .exceptionallyAccept {
                    when {
                        onTimeout != null && it is TimeoutException -> onTimeout.invoke()
                        else -> throw it
                    }
                }
        }

}
