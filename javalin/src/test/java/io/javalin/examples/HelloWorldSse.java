/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.http.sse.SseClient;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;


public class HelloWorldSse {

    public static void main(String[] args) throws InterruptedException {

        Queue<SseClient> clients = new ConcurrentLinkedQueue<>();

        Javalin app = Javalin.create(config -> {
            config.routes.get("/", ctx -> ctx.html("<script>new EventSource('http://localhost:7000/sse').addEventListener('hi', msg => console.log(msg));</script>"));
            config.routes.sse("/sse", client -> {
                client.keepAlive();
                clients.add(client); // save the sse to use outside of this context
                client.onClose(() -> clients.remove(client));
            });
        }).start(7000);

        while (true) {
            for (SseClient client : clients) {
                client.sendEvent("hi", "hello world");
            }
            TimeUnit.SECONDS.sleep(1);
        }

    }

}
