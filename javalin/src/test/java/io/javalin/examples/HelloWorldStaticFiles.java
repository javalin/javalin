/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.examples;

import io.javalin.Javalin;

public class HelloWorldStaticFiles {

    public static void main(String[] args) {
        Javalin.create(config -> {
            config.addStaticFiles("/public");
        }).start(7070);
    }

}
