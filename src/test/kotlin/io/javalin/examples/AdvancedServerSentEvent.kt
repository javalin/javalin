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

    val threadPool = QueuedThreadPool(100, 2, 60_000)
    val counterSse = ConcurrentLinkedQueue<EventSource>()
    val statsSse = ConcurrentLinkedQueue<EventSource>()
    var counter = 1
    var eventsSent = 0;

    Javalin.create().apply {
        enableStaticFiles("/public")
        server { Server(threadPool) }
        get("/") { it.redirect("/sse/sse-example.html")}
        sse("/sse-counter") { sse ->
            println("Adding new connection ...")
            counterSse.add(sse)
            sse.onClose { eventSource -> counterSse.remove(eventSource) }
        }
        sse("/sse-stats") { sse ->
            statsSse.add(sse)
            sse.onClose { eventSource -> statsSse.remove(eventSource) }
        }
    }.start(7000)

    while (true) {
        statsSse.forEach { it.sendEvent("stats", "Connections: ${counterSse.size + statsSse.size}, Threads: ${threadPool.busyThreads}/${threadPool.threads}, ", 999.toString()) }
        counterSse.forEach { it.sendEvent("counter", "Counter: ${counter++}", 1.toString()) }
        TimeUnit.SECONDS.sleep(1)
        println("Sending event ${eventsSent++}")
    }
}
