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
    Javalin.create { it.enableDevLogging() }.apply {
        ws("/websocket") { ws ->
            ws.onConnect { ctx ->
                println("Connection established")
                ctx.send("[MESSAGE FROM SERVER] Connection established")
            }
            ws.onMessage { ctx ->
                val message = ctx.message()
                println("Received: " + message)
                ctx.send("[MESSAGE FROM SERVER] Echo: " + message)
            }
            ws.onClose { ctx -> println("Closed") }
            ws.onError { ctx -> println("Errored") }
        }
        get("/") { ctx ->
            ctx.html("""<h1>WebSocket example</h1>
                <script>
                    let ws = new WebSocket("ws://localhost:7070/websocket");
                    ws.onmessage = e => document.body.insertAdjacentHTML("beforeEnd", "<pre>" + e.data + "</pre>");
                    ws.onclose = () => alert("WebSocket connection closed");
                    setInterval(() => ws.send("Repeating request every 2 seconds"), 2000);
                </script>""")
        }
    }.start(7070)
}
