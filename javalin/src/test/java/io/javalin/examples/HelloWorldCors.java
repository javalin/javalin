/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;

public class HelloWorldCors {

    public static void main(String[] args) {

        Javalin corsApp = Javalin.create(config -> {
            config.enableCorsForOrigin("http://localhost:7001/", "http://localhost:7002");
        }).start(7070);

        corsApp.get(ctx -> ctx.json("Hello Get"));
        corsApp.post(ctx -> ctx.json("Hello Post"));
        corsApp.patch(ctx -> ctx.json("Hello Patch"));

        Javalin.create().start(7001).get("/", ctx -> ctx.html("Try some CORS"));
        Javalin.create().start(7002).get("/", ctx -> ctx.html("Try some CORS"));
        Javalin.create().start(7003).get("/", ctx -> ctx.html("No CORS here"));

    }

}
