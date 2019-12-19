/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;

// WebSockets also work with ssl,
// see HelloWorldSecure for how to set that up
public class HelloWorldWebSockets {
    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> config.enableDevLogging());
        app.ws("/websocket", ws -> {
            ws.onConnect(ctx -> {
                System.out.println("Connected");
                ctx.send("[MESSAGE FROM SERVER] Connection established");
            });
            ws.onMessage(ctx -> {
                System.out.println("Received: " + ctx.message());
                ctx.send("[MESSAGE FROM SERVER] Echo: " + ctx.message());
            });
            ws.onClose(ctx -> {
                System.out.println("Closed");
            });
            ws.onError(ctx -> {
                System.out.println("Errored");
            });
        });
        app.get("/", ctx -> {
            ctx.html("<h1>WebSocket example</h1>\n" +
                "<script>\n" +
                "   let ws = new WebSocket(\"ws://localhost:7070/websocket\");\n" +
                "   ws.onmessage = e => document.body.insertAdjacentHTML(\"beforeEnd\", \"<pre>\" + e.data + \"</pre>\");\n" +
                "   ws.onclose = () => alert(\"WebSocket connection closed\");\n" +
                "   setInterval(() => ws.send(\"Repeating request every 2 seconds\"), 2000);\n" +
                "</script>");
        });
        app.start(7070);
    }
}
