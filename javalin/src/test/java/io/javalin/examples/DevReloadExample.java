/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;

/**
 * Example demonstrating the DevReloadPlugin for automatic change detection during development.
 * 
 * IMPORTANT: This plugin detects file changes but does NOT automatically reload classes.
 * For actual hot reload, you need to:
 * 
 * 1. Use your build tool's continuous compilation mode
 * 2. Watch the compiled output directory (target/classes or build/classes)
 * 3. Implement custom reload logic in the onReload callback
 * 
 * RECOMMENDED WORKFLOW:
 * 
 * Option A - With Maven:
 *   Terminal 1: mvn compile -DskipTests (set up watch mode)
 *   Terminal 2: Run this example
 *   
 * Option B - With Gradle:
 *   Terminal 1: gradle build --continuous
 *   Terminal 2: Run this example
 *   
 * Option C - Simple file watching (this example):
 *   Just run this example - it will detect file changes in source directories
 *   but won't automatically recompile or reload classes
 * 
 * For production-ready hot reload with class reloading, consider:
 * - JRebel (commercial)
 * - DCEVM + HotswapAgent (open source)
 * - Spring DevTools (if using Spring)
 * - Quarkus dev mode (if using Quarkus)
 * 
 * This plugin is useful for:
 * - Triggering notifications when files change
 * - Reloading configuration files
 * - Triggering custom build steps
 * - Working with build-tool-agnostic setups
 */
public class DevReloadExample {

    public static void main(String[] args) {
        var app = Javalin.create(config -> {
            // Enable dev reload with file watching
            config.bundledPlugins.enableDevReload(plugin -> {
                // Watch source directories for changes
                // For actual reload, watch compiled output: "target/classes", "build/classes/kotlin/main"
                plugin.watchPaths = java.util.List.of(
                    "src/main/java",
                    "src/main/kotlin",
                    "src/main/resources"
                );
                
                // Debounce delay (ms) - waits this long after last change before triggering
                plugin.debounceDelayMs = 500;
                
                // Enable verbose logging to see what's happening
                plugin.verbose = true;
                
                // Custom reload callback
                // NOTE: Full server restart requires careful thread management
                // This example just logs - implement your own restart logic as needed
                plugin.onReload = javalin -> {
                    System.out.println("\n=== CHANGE DETECTED ===");
                    System.out.println("To implement full reload:");
                    System.out.println("1. Stop the current Javalin instance");
                    System.out.println("2. Reload classes (requires custom classloader)");
                    System.out.println("3. Recreate and start a new Javalin instance");
                    System.out.println("4. Or use a build tool plugin for hot reload");
                    System.out.println("========================\n");
                };
            });

            // Optional: Enable dev logging to see request details
            config.bundledPlugins.enableDevLogging();
        });

        app.get("/", ctx -> ctx.result("Hello World! Edit files in watched directories to trigger detection."))
            .get("/users/{id}", ctx -> ctx.json(new User(ctx.pathParam("id"), "John Doe")))
            .start(7070);

        System.out.println("\n" + "=".repeat(70));
        System.out.println("Dev server started on http://localhost:7070");
        System.out.println("Watching directories: src/main/java, src/main/kotlin, src/main/resources");
        System.out.println("Edit files in these directories to see change detection in action.");
        System.out.println("=".repeat(70) + "\n");
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
