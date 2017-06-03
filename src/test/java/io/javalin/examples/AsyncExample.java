/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import javax.servlet.AsyncContext;

import io.javalin.Javalin;

public class AsyncExample {

    public static void main(String[] args) {

        Javalin app = Javalin.Companion.create().port(5454);

        app.get("/test-custom", (req, res) -> {
            AsyncContext asyncContext = req.unwrap().startAsync();
            simulateAsyncTask(() -> {
                res.status(418);
                asyncContext.complete();
            });
        });

        app.get("/test-async", (req, res) -> req.async(() -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            simulateAsyncTask(() -> {
                res.status(418);
                future.complete(null);
            });
            return future;
        }));

    }

    private static void simulateAsyncTask(Runnable runnable) {
        new Timer().schedule(
            new TimerTask() {
                public void run() {
                    runnable.run();
                }
            },
            1000
        );
    }

}
