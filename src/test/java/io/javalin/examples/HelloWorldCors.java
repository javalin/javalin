/*
* Javalin - https://javalin.io
* Copyright 2017 David Åse
* Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
*/

package io.javalin.examples;

import io.javalin.Javalin;

import static io.javalin.ApiBuilder.*;

public class HelloWorldCors {

    public static void main(String[] args) {

        Javalin corsApp = Javalin.create()
            .port(7000)
            .enableCorsForOrigin("http://localhost:7001/", "http://localhost:7002")
            .start();

        corsApp.routes(() -> {
            get(ctx -> ctx.json("Hello Get"));
            post(ctx -> ctx.json("Hello Post"));
            patch(ctx -> ctx.json("Hello Patch"));
        });

        Javalin.start(7001).get("/", ctx -> ctx.html("Try some CORS"));
        Javalin.start(7002).get("/", ctx -> ctx.html("Try some CORS"));
        Javalin.start(7003).get("/", ctx -> ctx.html("No CORS here"));

    }

}
