/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.patch;
import static io.javalin.apibuilder.ApiBuilder.post;

public class HelloWorldCors {

    public static void main(String[] args) {

        Javalin corsApp = Javalin.create(config -> {
            config.enableCorsForOrigin("http://localhost:7001/", "http://localhost:7002");
        }).start(7070);

        corsApp.routes(() -> {
            get(ctx -> ctx.json("Hello Get"));
            post(ctx -> ctx.json("Hello Post"));
            patch(ctx -> ctx.json("Hello Patch"));
        });

        Javalin.create().start(7001).get("/", ctx -> ctx.html("Try some CORS"));
        Javalin.create().start(7002).get("/", ctx -> ctx.html("Try some CORS"));
        Javalin.create().start(7003).get("/", ctx -> ctx.html("No CORS here"));

    }

}
