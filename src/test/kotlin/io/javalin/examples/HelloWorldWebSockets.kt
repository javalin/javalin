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
    val app = Javalin.create().port(7000)
    app.ws("/websocket") { ws ->
        ws.onConnect { session -> println("Connected") }
        ws.onMessage { message ->
            println("Received: " + message)
            ws.send("Echo: " + message)
        }
        ws.onClose { statusCode, reason -> println("Closed") }
        ws.onError { throwable -> println("Errored") }
    }
    app.start()
    app.get("/test", { ctx -> })
}
