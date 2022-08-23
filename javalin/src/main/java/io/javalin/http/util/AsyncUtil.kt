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

    fun submitAsyncTask(context: Context, executor: ExecutorService, timeout: Long, onTimeout: (() -> Unit)?, task: Runnable): CompletableFuture<*> {
        val await = CompletableFuture<Any?>()

        context.future(
            future = await,
            launch = {
                CompletableFuture.runAsync(task, executor)
                    .thenAccept { await.complete(null) }
                    .let { if (timeout > 0) it.orTimeout(timeout, MILLISECONDS) else it }
                    .exceptionallyAccept {
                        when {
                            onTimeout != null && it is TimeoutException -> onTimeout.invoke().run { await.complete(null) }
                            else -> await.completeExceptionally(it) // catch standard exception
                        }
                    }
                    .exceptionallyAccept { await.completeExceptionally(it) } // catch exception from timeout listener
            },
        )

        return await
    }

}
