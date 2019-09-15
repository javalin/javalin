/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.apibuilder.ApiBuilder.get
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val app = Javalin.create { it.enableDevLogging() }.start(7070)

    app.routes {
        get("/result") { ctx ->
            val future = CompletableFuture<String>()
            Executors.newSingleThreadScheduledExecutor()
                    .schedule({ future.complete("Hello World!") }, 10, TimeUnit.MILLISECONDS)
            ctx.result(future)
        }
        get("/json") { ctx ->
            val future = CompletableFuture<List<String>>()
            Executors.newSingleThreadScheduledExecutor()
                    .schedule({ future.complete(Arrays.asList("a", "b", "c")) }, 10, TimeUnit.MILLISECONDS)
            ctx.json(future)
        }
    }
}
