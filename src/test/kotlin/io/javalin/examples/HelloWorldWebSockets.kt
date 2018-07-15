/*
* Javalin - https://javalin.io
* Copyright 2017 David Åse
* Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
*/

package io.javalin.examples

import io.javalin.Javalin

// WebSockets also work with ssl,
// see HelloWorldSecure for how to set that up
fun main(args: Array<String>) {
    Javalin.create().apply {
        ws("/websocket") { ws ->
            ws.onConnect { session -> println("Connected") }
            ws.onMessage { session, message ->
                println("Received: " + message)
                session.send("Echo: " + message)
            }
            ws.onClose { session, statusCode, reason -> println("Closed") }
            ws.onError { session, throwable -> println("Errored") }
        }
    }.start(7070)
}
