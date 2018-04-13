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
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static java.util.stream.IntStream.range;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestAsync {

    private static Logger log = LoggerFactory.getLogger(TestAsync.class);

    @Test
    @Ignore("For running manually")
    public void test_async() throws Exception {

        QueuedThreadPool threadPool = new QueuedThreadPool(10, 2, 60_000);

        Javalin app = Javalin.create()
            .embeddedServer(new EmbeddedJettyFactory(() -> new Server(threadPool)))
            .port(0)
            .start();

        app.get("/test-async", ctx -> ctx.result(getFuture()));
        app.get("/test-sync", ctx -> ctx.result(getBlockingResult()));

        timeCallable("Async result", () -> {
            return new ForkJoinPool(100).submit(() -> range(0, 50).parallel().forEach(i -> {
                assertThat(getBody("http://localhost:" + app.port() + "/test-async"), is("success"));
            })).get();
        });

        timeCallable("Blocking result", () -> {
            return new ForkJoinPool(100).submit(() -> range(0, 50).parallel().forEach(i -> {
                assertThat(getBody("http://localhost:" + app.port() + "/test-sync"), is("success"));
            })).get();
        });

        app.stop();
    }

    private void timeCallable(String name, Callable callable) throws Exception {
        long startTime = System.currentTimeMillis();
        callable.call();
        log.info(name + " took " + (System.currentTimeMillis() - startTime) + " milliseconds");
    }

    private String getBlockingResult() throws InterruptedException {
        Thread.sleep(2000);
        return "success";
    }

    private CompletableFuture<String> getFuture() {
        CompletableFuture<String> future = new CompletableFuture<>();
        new Timer().schedule(
            new TimerTask() {
                public void run() {
                    future.complete("success");
                }
            },
            2000
        );
        return future;
    }

    private String getBody(String url) {
        try {
            return new SimpleHttpClient().http_GET(url).getBody();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

}
