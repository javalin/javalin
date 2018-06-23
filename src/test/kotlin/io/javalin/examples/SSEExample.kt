/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import io.javalin.serversentevent.EventSourceEmitter
import java.io.IOException
import java.util.ArrayList
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val emitters = ArrayList<EventSourceEmitter>()

    val app = Javalin.create().start(7000)
    app.get("/"
    ) { ctx ->
        ctx.html(
                ""
                        + "<script>" +
                        "var sse = new EventSource(\"http://localhost:7000/sse\");" +
                        "sse.addEventListener(\"hi\", data => console.log(data));"
                        + "</script>"
        )
    }

    app.sse("/sse", emitters)

    while (true) {
        val toRemove = ArrayList<EventSourceEmitter>()
        for (emitter in emitters) {
            try {
                emitter.emmit("hi", "hello world")
            } catch (e: IOException) {
                toRemove.add(emitter)
            }

        }
        emitters.removeAll(toRemove)

        TimeUnit.SECONDS.sleep(5)
    }

}
