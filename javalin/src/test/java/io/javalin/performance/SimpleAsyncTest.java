/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.performance;

import io.javalin.Javalin;
import io.javalin.testing.HttpUtil;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

import static io.javalin.testing.JavalinTestUtil.get;
import static java.util.stream.IntStream.range;
import static org.assertj.core.api.Assertions.assertThat;

public class SimpleAsyncTest {

    private static final Logger log = LoggerFactory.getLogger(SimpleAsyncTest.class);

    @Test
    @Disabled("For running manually")
    public void test_async() throws Exception {

        QueuedThreadPool threadPool = new QueuedThreadPool(10, 2, 60_000);
        threadPool.setName("poolname");
        Javalin app = Javalin.create(c -> c.jetty.threadPool = threadPool).start(0);
        assertThat(((QueuedThreadPool) app.jettyServer().server().getThreadPool()).getName()).isEqualTo("poolname");
        HttpUtil http = new HttpUtil(app.port());

        get(app, "/test-async", ctx -> ctx.future(this::getFuture));
        get(app, "/test-sync", ctx -> ctx.result(getBlockingResult()));

        timeCallable("Async result", () -> {
            return new ForkJoinPool(100).submit(() -> range(0, 50).parallel().forEach(i -> {
                assertThat(http.getBody("/test-async")).isEqualTo("success");
            })).get();
        });

        timeCallable("Blocking result", () -> {
            return new ForkJoinPool(100).submit(() -> range(0, 50).parallel().forEach(i -> {
                assertThat(http.getBody("/test-sync")).isEqualTo("success");
            })).get();
        });

        app.stop();
    }

    private void timeCallable(String name, Callable<?> callable) throws Exception {
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
        Executors.newSingleThreadScheduledExecutor().schedule(() -> future.complete("success"), 2000, TimeUnit.MILLISECONDS);
        return future;
    }

}
