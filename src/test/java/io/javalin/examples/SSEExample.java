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

public class SSEExample {

    public static void main(String[] args) throws InterruptedException {
        List<EventSource> eventSources = new ArrayList<EventSource>();

        Javalin app = Javalin.create().start( 7000 );
        app.get( "/", ctx ->
                ctx.html(
                        ""
                                + "<script>" +
                                "var sse = new EventSource(\"http://localhost:7000/sse/1\");" +
                                "sse.addEventListener(\"hi\", data => console.log(data));"
                                + "</script>"
                )
        );

        app.sse( "/sse/:id", sse -> {
            sse.onOpen( eventSource -> {
                eventSource.sendEvent( "connect", "Connected!" );
                eventSources.add( eventSource );
                return null;
            });
            sse.onClose( eventSource -> eventSources.remove( eventSource ));
        } );

        while (true) {
            for (EventSource sse: eventSources) {
                sse.sendEvent( "hi", "hello world" );
            }
            TimeUnit.SECONDS.sleep( 1 );
        }

    }

}
