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
        port(7000)
        ws("/websocket") { ws ->
            ws.onConnect { wsCtx -> println("Connected") }
            ws.onMessage { wsCtx ->
                println("Received: " + wsCtx.message)
                wsCtx.send("Echo: " + wsCtx.message)
            }
            ws.onClose { wsCtx -> println("Closed") }
            ws.onError { wsCtx -> println("Errored") }
        }
    }.start()
}
