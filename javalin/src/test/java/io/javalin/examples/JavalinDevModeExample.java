/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.util.JavalinDevMode;

/**
 * Example demonstrating the JavalinDevMode utility for automatic server restart.
 * 
 * This provides a zero-configuration dev mode experience:
 * 1. Just wrap your app creation in JavalinDevMode.runWithAutoRestart()
 * 2. Run your build tool in continuous mode (separate terminal)
 * 3. Changes are automatically detected and the server restarts
 * 
 * Requirements:
 * - Terminal 1: Run this example
 * - Terminal 2: Run `mvn compile -Ddev` or `gradle classes --continuous`
 * 
 * When you save changes to your Java files, the build tool will recompile them,
 * and this utility will automatically restart the server.
 */
public class JavalinDevModeExample {

    public static void main(String[] args) {
        JavalinDevMode.runWithAutoRestart(() -> {
            return Javalin.create(config -> {
                // Enable dev logging to see requests
                config.bundledPlugins.enableDevLogging();
            })
            .get("/", ctx -> ctx.result("Hello from dev mode! Try editing this message."))
            .get("/user/{id}", ctx -> {
                String id = ctx.pathParam("id");
                ctx.json(new User(id, "User " + id));
            })
            .start(7070);
        });
    }

    static class User {
        public String id;
        public String name;

        public User(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }
}
