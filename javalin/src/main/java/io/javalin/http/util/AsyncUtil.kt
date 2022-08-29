package io.javalin.http.util

import io.javalin.http.Context
import io.javalin.util.exceptionallyAccept
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException
import java.util.function.Supplier

typealias DoneListener<R> = (Result<R>) -> Unit
typealias TimeoutListener = () -> Unit

object AsyncUtil {

    /** Defines default [ExecutorService] used by [Context.future] */
    const val ASYNC_EXECUTOR_KEY = "javalin-context-async-executor"

    fun <R> submitAsyncTask(context: Context, executor: ExecutorService, task: Supplier<R>, onDone: DoneListener<R>?, timeout: Long, onTimeout: TimeoutListener?): Context =
        context.future {
            CompletableFuture.supplyAsync({ task.get() }, executor)
                .let { if (timeout > 0) it.orTimeout(timeout, MILLISECONDS) else it }
                .let { if (onDone != null) it.thenAccept { result -> onDone(Result.success(result)) } else it }
                .exceptionallyAccept {
                    when {
                        onTimeout != null && it is TimeoutException -> onTimeout.invoke()
                        else -> {
                            onDone?.invoke(Result.failure(it))
                            throw it
                        }
                    }
                }
        }

}
