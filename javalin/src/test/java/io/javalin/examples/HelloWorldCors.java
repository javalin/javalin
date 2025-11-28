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
        Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(corsConfig -> {
                    corsConfig.allowHost("http://localhost:7001/", "http://localhost:7002");
                });
            });
            config.routes.apiBuilder(() -> {
                get(ctx -> ctx.json("Hello Get"));
                post(ctx -> ctx.json("Hello Post"));
                patch(ctx -> ctx.json("Hello Patch"));
            });
        }).start(7070);

        Javalin.create(config -> config.routes.get("/", ctx -> ctx.html("Try some CORS"))).start(7001);
        Javalin.create(config -> config.routes.get("/", ctx -> ctx.html("Try some CORS"))).start(7002);
        Javalin.create(config -> config.routes.get("/", ctx -> ctx.html("No CORS here"))).start(7003);
    }

}
