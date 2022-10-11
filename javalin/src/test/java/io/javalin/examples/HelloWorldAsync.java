/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;

public class HelloWorldAsync {

    public static void main(String[] args) {
        try (Javalin app = Javalin.create().start(7070)) {
            app.get("/", ctx -> {
                ctx.async(
                    () -> {
                        Thread.sleep(1000);
                        return "Hello world";
                    },
                    (result, $) -> ctx.result(result)
                );
            });
        }
    }

}
