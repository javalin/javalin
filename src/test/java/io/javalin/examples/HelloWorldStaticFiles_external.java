/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.staticfiles.Location;

public class HelloWorldStaticFiles_external {

    public static void main(String[] args) {
        Javalin.create()
            .port(7070)
            .enableStaticFiles("src/test/external/", Location.EXTERNAL)
            .start();
    }

}
