/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Ignore;
import org.junit.Test;

import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

public class TestAsync {

    @Test
    @Ignore("Just for running manually")
    public void test_async() throws Exception {

        AtomicInteger requestNum = new AtomicInteger(0);

        Javalin app = Javalin.create()
            .embeddedServer(new EmbeddedJettyFactory(() -> new Server(new QueuedThreadPool(16, 10, 60_000))))
            .port(5454)
            .start()
            .awaitInitialization();

        app.get("/", (req, res) -> {
            int num = requestNum.getAndIncrement();
            System.out.println("Threads:" + app.embeddedServer().activeThreadCount() + " - Request #" + num);
            try {
                Thread.sleep(5_000);
                res.body("res");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        long startTime = System.currentTimeMillis();
        ForkJoinPool forkJoinPool = new ForkJoinPool(100);
        forkJoinPool.submit(
            () -> IntStream.range(0, 50).parallel().forEach(i -> {
                try {
                    System.out.println("Call #" + i);
                    call(5454, "/");
                } catch (UnirestException e) {
                    e.printStackTrace();
                }
            })
        ).get();
        System.out.println("took " + (System.currentTimeMillis() - startTime) + " milliseconds");
    }

    private String call(int port, String path) throws UnirestException {
        return Unirest.get("http://localhost:" + port + path).asString().getBody();
    }

}
