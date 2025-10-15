/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class HelloWorldFuture {

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.routes.get("/", ctx -> {
                CompletableFuture<String> future = new CompletableFuture<>();
                Executors.newSingleThreadScheduledExecutor().schedule(() -> future.complete("Hello World!"), 10, TimeUnit.MILLISECONDS);
                ctx.future(() -> future.thenApply(ctx::result));
            });
        }).start(7070);
    }

}
