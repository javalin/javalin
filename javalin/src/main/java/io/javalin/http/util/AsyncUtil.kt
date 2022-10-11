package io.javalin.http.util

import io.javalin.http.Context
import io.javalin.util.ConcurrencyUtil
import io.javalin.util.function.ThrowingSupplier
import jakarta.servlet.AsyncContext
import jakarta.servlet.AsyncEvent
import jakarta.servlet.AsyncListener
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeoutException
import kotlin.Exception

fun interface DoneListener<R> {
    fun call(result: R?, exception: Exception?)
}

internal object AsyncUtil {

    val defaultExecutor = ConcurrencyUtil.executorService("JavalinDefaultAsyncThreadPool")

    fun <R> submitAsyncTask(
        context: Context,
        executor: ExecutorService?,
        timeout: Long,
        onTimeout: Runnable?,
        onDone: DoneListener<R>?,
        task: ThrowingSupplier<R, Exception>
    ): Unit =
        context.future {
            CompletableFuture.supplyAsync({ task.get() }, executor ?: defaultExecutor)
                .let { if (timeout > 0) it.orTimeout(timeout, MILLISECONDS) else it }
                .let { if (onDone != null) it.thenAccept { result -> onDone.call(result, null) } else it }
                .exceptionally {
                    when {
                        onTimeout != null && it is TimeoutException -> {
                            onTimeout.run()
                            null // handled
                        }
                        onDone != null && it is CompletionException && it.cause is Exception -> {
                            onDone.call(null, it.cause as Exception)
                            null // handled
                        }
                        else -> throw it // rethrow if not handled by any listener
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
