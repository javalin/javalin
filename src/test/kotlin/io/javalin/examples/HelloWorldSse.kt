/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.serversentevent.EventSource
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {

    val eventSources = ConcurrentLinkedQueue<EventSource>()

    val app = Javalin.create().start(7000)
    app.get("/") { ctx -> ctx.html("<script>new EventSource('http://localhost:7000/sse').addEventListener('hi', msg => console.log(msg));") }

    app.sse("/sse") { sse ->
        eventSources.add(sse) // save the sse to use outside of this context
        sse.onClose { eventSources.remove(sse) }
    }

    while (true) {
        for (sse in eventSources) {
            sse.sendEvent("hi", "hello world")
        }
        TimeUnit.SECONDS.sleep(1)
    }
}
