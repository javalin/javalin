/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.examples;

import io.javalin.Javalin;

import static io.javalin.ApiBuilder.*;

public class HelloWorldApi {

    public static void main(String[] args) {
        Javalin.create()
            .setPort(7070)
            .start()
            .routes(() -> {
                get("/hello", ctx -> ctx.result("Hello World"));
                path("/api", () -> {
                    get("/test", ctx -> ctx.result("Hello World"));
                    get("/tast", ctx -> ctx.status(200).result("Hello world"));
                    get("/hest", ctx -> ctx.status(200).result("Hello World"));
                    get("/hast", ctx -> ctx.status(200).result("Hello World").header("test", "tast"));
                });
            });
    }

}
