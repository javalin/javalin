/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.http.sse.SseClient
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {

    val clients = ConcurrentLinkedQueue<SseClient>()

    val app = Javalin.create().start(7000)
    app.get("/") { ctx -> ctx.html("<script>new EventSource('http://localhost:7000/sse').addEventListener('hi', msg => console.log(msg));</script>") }

    app.sse("/sse") { client ->
        clients.add(client) // save the sse to use outside of this context
        client.onClose { clients.remove(client) }
    }

    while (true) {
        for (client in clients) {
            client.sendEvent("hi", "hello world")
        }
        TimeUnit.SECONDS.sleep(1)
    }
}
