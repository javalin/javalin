/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory;
import io.javalin.util.SimpleHttpClient;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.IntStream;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestAsync {

    private static Logger log = LoggerFactory.getLogger(TestAsync.class);

    @Test
    @Ignore("Just for running manually")
    public void test_async() throws Exception {

        Javalin app = Javalin.create()
            .embeddedServer(new EmbeddedJettyFactory(() -> new Server(new QueuedThreadPool(16, 10, 60_000))))
            .port(5454)
            .start();

        app.get("/test-async", ctx -> ctx.async(() -> {
            CompletableFuture<Void> future = new CompletableFuture<>();
            new Timer().schedule(
                new TimerTask() {
                    public void run() {
                        ctx.status(418);
                        future.complete(null);
                    }
                },
                1000
            );
            return future;
        }));

        long startTime = System.currentTimeMillis();
        ForkJoinPool forkJoinPool = new ForkJoinPool(200);
        forkJoinPool.submit(
            () -> IntStream.range(0, 50).parallel().forEach(i -> {
                try {
                    assertThat(new SimpleHttpClient().http_GET("http://localhost:5454/test-async").getStatus(), is(418));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            })
        ).get();
        log.info("took " + (System.currentTimeMillis() - startTime) + " milliseconds");

        app.stop();
    }

}
