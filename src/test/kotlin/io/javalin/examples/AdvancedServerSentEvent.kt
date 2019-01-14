/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.serversentevent.EventSource
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.QueuedThreadPool
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {

    val tp = QueuedThreadPool(8, 2, 60_000)
    val counterSse = ConcurrentLinkedQueue<EventSource>()
    val statsSse = ConcurrentLinkedQueue<EventSource>()

    Javalin.create().apply {
        enableStaticFiles("/public")
        server { Server(tp) }
        get("/") { it.redirect("/sse/sse-example.html") }
        sse("/sse-counter") { sse ->
            counterSse.add(sse)
            sse.onClose { counterSse.remove(sse) }
        }
        sse("/sse-stats") { sse ->
            statsSse.add(sse)
            sse.onClose { statsSse.remove(sse) }
        }
    }.start(7000)

    for (counter in 1..999) {
        counterSse.forEach {
            it.sendEvent("Counter: $counter") // send as "message"
            it.sendEvent("counter", "Counter: $counter", 1.toString())
        }
        statsSse.forEach {
            it.sendEvent("stats", "Clients: ${counterSse.size + statsSse.size}, Threads: ${tp.busyThreads}/${tp.threads}", 999.toString())
        }
        TimeUnit.SECONDS.sleep(1)
    }
}
