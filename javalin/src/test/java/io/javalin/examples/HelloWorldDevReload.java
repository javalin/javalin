/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;

import java.util.Map;

/**
 * Demonstrates the DevReloadPlugin for automatic route reloading during development.
 * The plugin checks for file changes on each incoming HTTP request. If changes are detected,
 * it re-executes your Javalin.create() config to rebuild the routing tables.
 * The server stays running on the same port — only the routing tables are swapped.
 *
 * To see it in action:
 * 1. Start this application
 * 2. Change a route handler below (e.g., change "Hello World!" to something else)
 * 3. Save the file — the plugin compiles and reloads automatically on next request
 * 4. Make an HTTP request — the plugin detects the change and reloads before responding
 *
 * Try these URLs:
 *   http://localhost:7070/              → Hello World!
 *   http://localhost:7070/users/42      → User 42
 *   http://localhost:7070/users/abc     → 400 bad request (exception handler)
 *   http://localhost:7070/crash         → 500 internal error (exception handler)
 *   http://localhost:7070/nonexistent   → 404 custom error page (error handler)
 */
public class HelloWorldDevReload {

    public static void main(String[] args) {
        Javalin.create(config -> {

            // --- Routes ---
            config.routes.get("/", ctx -> ctx.result("Hello World!"));

            config.routes.get("/users/{id}", ctx -> {
                String id = ctx.pathParam("id");
                try {
                    int userId = Integer.parseInt(id);
                    ctx.json(Map.of("id", userId, "name", "User " + userId));
                } catch (NumberFormatException e) {
                    throw new BadRequestResponse("Invalid user id: " + id);
                }
            });

            config.routes.get("/crash", ctx -> {
                throw new RuntimeException("Something went wrong!");
            });

            // --- Before/After handlers ---
            config.routes.before(ctx -> ctx.header("X-Dev-Reload", "active"));
            config.routes.after(ctx -> ctx.header("X-Request-Time", System.currentTimeMillis() + "ms"));

            // --- Exception handlers ---
            config.routes.exception(BadRequestResponse.class, (e, ctx) -> {
                ctx.status(400).json(Map.of("error", e.getMessage()));
            });

            config.routes.exception(NotFoundResponse.class, (e, ctx) -> {
                ctx.status(404).json(Map.of("error", "Not found"));
            });

            config.routes.exception(Exception.class, (e, ctx) -> {
                ctx.status(500).json(Map.of("error", "Internal error", "message", e.getMessage()));
            });

            // --- Error handlers ---
            config.routes.error(404, ctx -> {
                ctx.json(Map.of("error", "Page not found", "path", ctx.path()));
            });

            // --- Dev reload ---
            config.bundledPlugins.enableDevReload();
            // For projects needing annotation processors or non-javac compilers:
            // config.bundledPlugins.enableDevReload(reload -> {
            //     reload.compileCommand = "mvn compile -o -q --batch-mode";
            // });

        }).start(7070);
    }

}
