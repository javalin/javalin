package io.javalin.http.util

import io.javalin.http.Context
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException
import java.util.function.Supplier

typealias DoneListener<R> = (Result<R>) -> Unit
typealias TimeoutListener = () -> Unit

internal object AsyncUtil {

    /** Defines default [ExecutorService] used by [Context.future] */
    const val ASYNC_EXECUTOR_KEY = "javalin-context-async-executor"

    fun <R> submitAsyncTask(context: Context, executor: ExecutorService, task: Supplier<R>, onDone: DoneListener<R>?, timeout: Long, onTimeout: TimeoutListener?) =
        context.future {
            CompletableFuture.supplyAsync({ task.get() }, executor)
                .let { if (timeout > 0) it.orTimeout(timeout, MILLISECONDS) else it }
                .let { if (onDone != null) it.thenAccept { result -> onDone(Result.success(result)) } else it }
                .exceptionally {
                    when {
                        onTimeout != null && it is TimeoutException -> {
                            onTimeout.invoke()
                            null // handled
                        }
                        else -> {
                            onDone?.invoke(Result.failure(it))
                            throw it // rethrow
                        }
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
