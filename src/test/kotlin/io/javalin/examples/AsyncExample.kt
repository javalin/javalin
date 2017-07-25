/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import java.util.*
import java.util.concurrent.CompletableFuture


fun main(args: Array<String>) {

    val app = Javalin.create().port(5454).start()

    app.get("/test-custom") { ctx ->
        val asyncContext = ctx.request().startAsync()
        simulateAsyncTask({
            ctx.status(418)
            asyncContext.complete()
        })
    }

    app.get("/test-async") { ctx ->
        ctx.async {
            val future = CompletableFuture<Void>()
            simulateAsyncTask({
                ctx.status(418)
                future.complete(null)
            })
            future
        }
    }

}

private fun simulateAsyncTask(runnable: () -> Unit) {
    Timer().schedule(
            object : TimerTask() {
                override fun run() {
                    runnable.invoke()
                }
            },
            1000
    )
}
