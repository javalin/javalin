/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.examples;

import io.javalin.Javalin;

public class HelloWorldApi {

    public static void main(String[] args) {
        Javalin.create()
            .start(7070)
            .get("/hello", ctx -> ctx.result("Hello World"))
            .path("/api", router -> {
                router.get("/test", ctx -> ctx.result("Hello World"));
                router.get("/tast", ctx -> ctx.status(200).result("Hello world"));
                router.get("/hest", ctx -> ctx.status(200).result("Hello World"));
                router.get("/hast", ctx -> ctx.status(200).result("Hello World").header("test", "tast"));
            });
    }

}
