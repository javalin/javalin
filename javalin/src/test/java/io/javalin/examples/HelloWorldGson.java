/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.json.JavalinGson;
import java.util.Arrays;

public class HelloWorldGson {

    public static void main(String[] args) {
        Javalin.create(config -> config.jsonMapper(new JavalinGson()))
            .get("/", ctx -> ctx.json(Arrays.asList("a", "b", "c")))
            .start(7070);
    }

}
