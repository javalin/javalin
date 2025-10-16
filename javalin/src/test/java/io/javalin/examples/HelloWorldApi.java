/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.examples;

import io.javalin.Javalin;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.http.HttpStatus.OK;

public class HelloWorldApi {

    public static void main(String[] args) {
        Javalin.create(config -> {
            config.routes.apiBuilder(() -> {
                get("/hello", ctx -> ctx.result("Hello World"));
                path("/api", () -> {
                    get("/test", ctx -> ctx.result("Hello World"));
                    get("/tast", ctx -> ctx.status(OK).result("Hello world"));
                    get("/hest", ctx -> ctx.status(OK).result("Hello World"));
                    get("/hast", ctx -> ctx.status(OK).result("Hello World").header("test", "tast"));
                });
            });
        })
        .start(7070);
    }

}
