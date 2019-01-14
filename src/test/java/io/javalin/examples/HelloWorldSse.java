/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.serversentevent.EventSource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HelloWorldSse {

    public static void main(String[] args) throws InterruptedException {

        List<EventSource> eventSources = new ArrayList<>();

        Javalin app = Javalin.create().start(7000);
        app.get("/", ctx ->
            ctx.html("" +
                "<script>" +
                "var sse = new EventSource('http://localhost:7000/sse');" +
                "sse.addEventListener('hi', data => console.log(data));" +
                "</script>" +
                "")
        );

        app.sse("/sse", sse -> {
            sse.sendEvent("connect", "Connected!");
            eventSources.add(sse);
            sse.onClose(eventSources::remove);
        });

        while (true) {
            for (EventSource sse : eventSources) {
                sse.sendEvent("hi", "hello world");
            }
            TimeUnit.SECONDS.sleep(1);
        }

    }

}
