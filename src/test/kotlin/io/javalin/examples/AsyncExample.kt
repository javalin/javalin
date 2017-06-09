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

    val app = Javalin.create().port(5454)

    app.get("/test-custom") { req, res ->
        val asyncContext = req.unwrap().startAsync()
        simulateAsyncTask({
            res.status(418)
            asyncContext.complete()
        })
    }

    app.get("/test-async") { req, res ->
        req.async {
            val future = CompletableFuture<Void>()
            simulateAsyncTask({
                res.status(418)
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
