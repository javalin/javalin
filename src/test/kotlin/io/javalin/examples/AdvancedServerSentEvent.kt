/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.http.sse.SseClient
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.util.thread.QueuedThreadPool
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {

    val tp = QueuedThreadPool(8, 2, 60_000)
    val counterClients = ConcurrentLinkedQueue<SseClient>()
    val statsClients = ConcurrentLinkedQueue<SseClient>()

    Javalin.create {
        it.addStaticFiles("/public")
        it.server { Server(tp) }
    }.apply {
        get("/") { it.redirect("/sse/sse-example.html") }
        sse("/sse-counter") { client ->
            counterClients.add(client)
            client.onClose { counterClients.remove(client) }
        }
        sse("/sse-stats") { eventSource ->
            statsClients.add(eventSource)
            eventSource.onClose { statsClients.remove(eventSource) }
        }
    }.start(7000)

    for (counter in 1..999) {
        counterClients.forEach {
            it.sendEvent("Counter: $counter") // send as "message"
            it.sendEvent("counter", "Counter: $counter", 1.toString())
        }
        statsClients.forEach {
            it.sendEvent("stats", "Clients: ${counterClients.size + statsClients.size}, Threads: ${tp.busyThreads}/${tp.threads}", 999.toString())
        }
        TimeUnit.SECONDS.sleep(1)
    }
}
