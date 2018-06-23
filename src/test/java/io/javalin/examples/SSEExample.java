/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.serversentevent.EventSourceEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SSEExample {
    static int i = 0;

    public static void main(String[] args) throws InterruptedException {

        List<EventSourceEmitter> emitters = new ArrayList<EventSourceEmitter>();

        Javalin app = Javalin.create().start( 7000 );
        app.get( "/", ctx ->
                ctx.html(
                        ""
                                + "<script>" +
                                "var sse = new EventSource(\"http://localhost:7000/sse\");" +
                                "sse.addEventListener(\"hi\", data => console.log(data));"
                                + "</script>"
                )
        );

        app.sse( "/sse", emitters );

        while (true) {
            List<EventSourceEmitter> toRemove = new ArrayList<EventSourceEmitter>();
            for (EventSourceEmitter emitter: emitters) {
                try {
                    emitter.emmit( "hi", "hello world" );
                } catch (IOException e) {
                    toRemove.add( emitter );
                }
            }
            emitters.removeAll( toRemove );

            TimeUnit.SECONDS.sleep( 5 );
        }

    }

}
