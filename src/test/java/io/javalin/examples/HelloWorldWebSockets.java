/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;

public class HelloWorldWebSockets {
    public static void main(String[] args) {
        Javalin app = Javalin.create().port(7000);
        app.get("/", ctx -> ctx.result("Hello World"));
        app.ws("/path", ws -> {
            ws.onConnect(session -> {
                System.out.println("Connected");
            });
            ws.onMessage(message -> {
                System.out.println("Messaged: " + message);
                ws.send(message);
            });
            ws.onClose((statusCode, reason) -> {
                System.out.println("Closed");
            });
            ws.onError(throwable -> {
                System.out.println("Errored");
            });
        }).start();
    }
}
