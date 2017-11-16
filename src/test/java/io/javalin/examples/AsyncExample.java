/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import javax.servlet.AsyncContext;

public class AsyncExample {

    public static void main(String[] args) {

        Javalin app = Javalin.create().port(5454).start();

        app.get("/test-custom", ctx -> {
            AsyncContext asyncContext = ctx.request().startAsync();
            simulateAsyncTask(() -> {
                ctx.status(418);
                asyncContext.complete();
            });
        });

        app.get("/test-async", ctx -> ctx.async(() -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            simulateAsyncTask(() -> {
                ctx.status(418);
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
