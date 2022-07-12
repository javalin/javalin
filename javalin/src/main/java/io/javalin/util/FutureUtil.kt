package io.javalin.util

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.function.Consumer

/** [CompletableFuture.thenAccept] alternative for [CompletableFuture.exceptionally] */
fun CompletableFuture<*>.exceptionallyAccept(exceptionConsumer: Consumer<Throwable>): CompletableFuture<*> =
    exceptionally {
        exceptionConsumer.accept(it)
        null
    }

/** [CompletableFuture.exceptionallyCompose] method is available since JDK12+, so we need a fallback for JDK11 */
fun <T> CompletableFuture<T>.exceptionallyComposeFallback(mapping: (Throwable) -> CompletionStage<T>): CompletableFuture<T> =
    thenApply { CompletableFuture.completedFuture(it) as CompletionStage<T> }
        .exceptionally { mapping(it) }
        .thenCompose { it }

fun <T> CompletableFuture<T>.isCompletedSuccessfully() = this.isDone && !this.isCompletedExceptionally && !this.isCancelled
