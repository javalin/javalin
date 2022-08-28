package io.javalin.http.util

import io.javalin.http.Context
import io.javalin.util.ConcurrencyUtil
import java.util.concurrent.CompletableFuture

object AsyncUtil {
    private val executor = ConcurrencyUtil.executorService("JavalinDefaultAsyncThreadPool")
    fun submit(ctx: Context, runnable: Runnable): CompletableFuture<Void> {
        val await = CompletableFuture<Void>()
        ctx.future(await)
        CompletableFuture.runAsync({
            try {
                runnable.run()
                await.complete(null)
            } catch (throwable: Throwable) {
                await.completeExceptionally(throwable)
            }
        }, executor)
        return await
    }
}
