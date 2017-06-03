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
        Javalin.Companion.create()
            .port(7070)
            .routes(() -> {
                get("/hello", (req, res) -> res.body("Hello World"));
                path("/api", () -> {
                    get("/test", (req, res) -> res.body("Hello World"));
                    get("/tast", (req, res) -> res.status(200).body("Hello world"));
                    get("/hest", (req, res) -> res.status(200).body("Hello World"));
                    get("/hast", (req, res) -> res.status(200).body("Hello World").header("test", "tast"));
                });
            });
    }

}
