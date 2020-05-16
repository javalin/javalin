/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples

import io.javalin.Javalin
import java.nio.ByteBuffer

fun main() {
    val app = Javalin.create {
        it.setWsSubProtocols(listOf("mqtt"))
    }

    app.apply {
        ws("/websocket") { ws ->
            ws.onConnect {
                println("Connection established")
            }
            ws.onBinaryMessage { ctx ->
                // for test, we don't check the received message. just return mqtt CONNACK directly
                // MQTT connack: 20 02 00 00
                val connack = byteArrayOf(20, 2, 0, 0)
                ctx.send(ByteBuffer.wrap(connack))
                println("Received CONNECT && sent CONNACK")
            }
            ws.onClose { println("Closed") }
            ws.onError { println("Errored") }
        }
        get("/mqtt") { ctx ->
            ctx.html("""
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
<h1>example on: setWsSubProtocols(listOf("mqtt")) </h1>

<script src="https://cdnjs.cloudflare.com/ajax/libs/paho-mqtt/1.0.1/mqttws31.min.js" type="text/javascript"></script>
<script>
    // Create a client instance
    client = new Paho.MQTT.Client("127.0.0.1", 7070, "/websocket", "pahoClient");

    // set callback handlers
    client.onConnectionLost = onConnectionLost;
    client.connect({onSuccess: onConnect});
    function onConnect() {
        console.log("onConnect");
        // Once a connection has been made, make a subscription and send a message.
        // client.subscribe("/World");
        // message = new Paho.MQTT.Message("Hello");
        // message.destinationName = "/World";
        // client.send(message);
    }
    function onConnectionLost(responseObject) {
        if (responseObject.errorCode !== 0) {
            console.log("onConnectionLost:" + responseObject.errorMessage);
        }
    }
</script>

</body>
</html>
                """)
        }.start(7070)
    }
}
