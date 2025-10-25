/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.plugin.BasicAuthPlugin;

public class HelloWorldBasicAuth {

    public static void main(String[] args) {
        Javalin
            .create(config -> {
                config.registerPlugin(new BasicAuthPlugin(basicAuthCfg -> {
                    basicAuthCfg.username = "panda";
                    basicAuthCfg.password = "bamboo";
                }));
                config.registerPlugin(
                    new BasicAuthPlugin(basicAuthCfg -> {
                        basicAuthCfg.username = "panda";
                        basicAuthCfg.password = "bamboo";
                    })
                );
                config.routes.get("/hello", ctx -> ctx.result("Hello World 1"));
            })
            .start(7070);
    }

}
