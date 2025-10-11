/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;

/**
 * Example demonstrating the DevReloadPlugin for automatic server reload during development.
 * 
 * This plugin watches specified directories for file changes and can trigger a reload
 * when changes are detected. This is useful during development to see changes without
 * manually restarting the server.
 * 
 * Note: This is a basic file watching implementation. For production-grade hot reload,
 * consider using build tool plugins (Maven/Gradle) that support class reloading.
 */
public class DevReloadExample {

    public static void main(String[] args) {
        var app = Javalin.create(config -> {
            // Enable dev reload with file watching
            config.bundledPlugins.enableDevReload(plugin -> {
                // Specify directories to watch for changes
                plugin.watchPaths = java.util.List.of(
                    "src/main/java",
                    "src/main/kotlin",
                    "src/main/resources"
                );
                
                // Set debounce delay (ms) to wait after detecting changes
                // before triggering reload (prevents multiple rapid reloads)
                plugin.debounceDelayMs = 500;
                
                // Enable verbose logging to see what's happening
                plugin.verbose = true;
                
                // Optional: Set custom reload callback
                // Note: Implementing full restart requires careful thread management
                plugin.onReload = javalin -> {
                    System.out.println("Change detected! In a full implementation, you would:");
                    System.out.println("1. Recompile changed files (using your build tool)");
                    System.out.println("2. Reload classes (using a classloader)");
                    System.out.println("3. Re-register routes with updated handlers");
                };
            });

            // Enable dev logging to see request details
            config.bundledPlugins.enableDevLogging();
        });

        app.get("/", ctx -> ctx.result("Hello World! Edit this file to see reload detection."))
            .get("/users/{id}", ctx -> ctx.json(new User(ctx.pathParam("id"), "John Doe")))
            .start(7070);

        System.out.println("\nDev server started on http://localhost:7070");
        System.out.println("Watching for file changes in: src/main/java, src/main/kotlin, src/main/resources");
        System.out.println("Edit files in these directories to trigger reload detection.\n");
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
