/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main() {
    val app = Javalin.create { it.plugins.enableDevLogging() }.start(7070)
    val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()

    app.routes {
        get("/result") { ctx ->
            ctx.future {
                val future = CompletableFuture<String>()
                scheduledExecutor.schedule({ future.complete("Hello World!") }, 10, TimeUnit.MILLISECONDS)
                future.thenApply { ctx.result(it) }
            }
        }
        get("/json") { ctx ->
            ctx.future {
                val future = CompletableFuture<List<String>>()
                scheduledExecutor.schedule({ future.complete(listOf("a", "b", "c")) }, 10, TimeUnit.MILLISECONDS)
                future.thenApply { ctx.json(it) }
            }
        }
    }
}
