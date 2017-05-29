/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory;

public class TestAsync {

    private static Logger log = LoggerFactory.getLogger(TestAsync.class);

    public static void main(String[] args) throws Exception {

        AtomicInteger requestNum = new AtomicInteger(0);

        Javalin app = Javalin.create()
            .embeddedServer(new EmbeddedJettyFactory(() -> new Server(new QueuedThreadPool(12, 8, 60_000))))
            .port(5454)
            .start()
            .awaitInitialization();

        app.get("/", (req, res) -> {
            int num = requestNum.getAndIncrement();
            log.info(timeStamp() + "Threads:" + app.embeddedServer().activeThreadCount() + " - Request #" + num);
            try {
                Thread.sleep(500);
                res.body("res");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

    }

    private static String timeStamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:MM:SS")) + ": ";
    }

    // run tests using node and artillery
    // npm install -g artillery
    // artillery quick --duration 60 --rate 10 -n 20 http://localhost:5454

}
