/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;

public class SSEExample {
    static int i = 0;

    public static void main(String[] args) {

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
        app.get( "/sse", ctx -> {
                    i++;
                    ctx.sse( String.valueOf( i ), "hi", "Hello World!" );
                } );

    }

}
