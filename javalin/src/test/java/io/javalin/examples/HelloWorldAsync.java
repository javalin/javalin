/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;

public class HelloWorldAsync {

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.routes.get("/", ctx -> {
                ctx.async(
                    (asyncConfig) -> {
                        asyncConfig.timeout = 1000L;
                        asyncConfig.onTimeout(timeoutCtx -> timeoutCtx.result("Request timed out :<"));
                    },
                    () -> {
                        Thread.sleep((long) (Math.random() * 2000L));
                        ctx.result("Hello Javalin");
                    }
                );
            });
        }).start(7070);
    }

}
