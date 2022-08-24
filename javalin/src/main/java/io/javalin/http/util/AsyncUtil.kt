package io.javalin.http.util

import io.javalin.http.Context
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.function.Consumer

object AsyncUtil {

    /** Defines default [ExecutorService] used by [Context.future] */
    const val ASYNC_EXECUTOR_KEY = "javalin-context-async-executor"

    fun submitAsyncTask(context: Context, executor: ExecutorService, timeout: Long, onTimeout: (() -> Unit)?, task: Runnable): CompletableFuture<*> {
        val await = CompletableFuture<Nothing>()

        context.future(
            future = await,
            launch = {
                CompletableFuture.runAsync(task, executor)
                    .thenAccept { await.complete(null) }
                    .orTimeoutIfTimeoutSet(timeout)
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

    /** [CompletableFuture.thenAccept] alternative for [CompletableFuture.exceptionally] */
    private fun CompletableFuture<*>.exceptionallyAccept(exceptionConsumer: Consumer<Throwable>): CompletableFuture<*> =
        exceptionally {
            exceptionConsumer.accept(it)
            null
        }

    fun <T> CompletableFuture<T>.orTimeoutIfTimeoutSet(timeout: Long) = this.apply { if (timeout > 0) this.orTimeout(timeout, TimeUnit.MILLISECONDS) }

}
