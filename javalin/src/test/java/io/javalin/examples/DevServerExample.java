/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;

/**
 * Simple Javalin application for testing the Development Server.
 * 
 * This application requires NO modifications to work with DevServer.
 * Just run it normally or use DevServer for auto-reload:
 * 
 * Normal run:
 *   mvn exec:java -Dexec.mainClass="io.javalin.examples.DevServerExample"
 * 
 * With DevServer:
 *   java -cp "target/classes:..." io.javalin.util.DevServer io.javalin.examples.DevServerExample
 */
public class DevServerExample {

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            // Optional: Enable dev logging to see requests
            config.bundledPlugins.enableDevLogging();
        })
        .get("/", ctx -> ctx.result("Hello from Javalin! Edit this message and save to see auto-reload."))
        .get("/api/status", ctx -> ctx.json(new Status("running", System.currentTimeMillis())))
        .start(7070);

        System.out.println("\nServer started at http://localhost:7070");
        System.out.println("Try editing this file and saving to see auto-reload in action!\n");
    }

    static class Status {
        public String status;
        public long timestamp;

        public Status(String status, long timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }
    }
}
