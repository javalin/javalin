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
        Javalin app = Javalin.create().port(7000);
        app.ws("/websocket", ws -> {
            ws.onConnect(session -> {
                System.out.println("Connected");
            });
            ws.onMessage(message -> {
                System.out.println("Received: " + message);
                ws.send("Echo: " + message);
            });
            ws.onClose((statusCode, reason) -> {
                System.out.println("Closed");
            });
            ws.onError(throwable -> {
                System.out.println("Errored");
            });
        });
        app.start();
    }
}
