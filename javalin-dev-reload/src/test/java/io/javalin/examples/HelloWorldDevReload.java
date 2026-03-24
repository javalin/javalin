/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;
import io.javalin.plugin.bundled.DevReloadPlugin;

import java.util.Map;

/**
 * Demonstrates the DevReloadPlugin for automatic reloading during development.
 *
 * The plugin runs a reverse proxy on your port and your app as a child process.
 * When files change, it recompiles (via your build tool) and restarts the child process.
 * The proxy keeps a stable port so the browser sees a seamless reload.
 *
 * To see it in action:
 * 1. Start this application
 * 2. Change a route handler below (e.g., change "Hello World!" to something else)
 * 3. Save the file — the plugin detects the change, recompiles, and restarts
 * 4. Refresh your browser — the new response appears
 *
 * Try these URLs:
 *   http://localhost:7070/              → Hello World!
 *   http://localhost:7070/users/42      → User 42
 *   http://localhost:7070/users/abc     → 400 bad request (exception handler)
 *   http://localhost:7070/crash         → 500 internal error (exception handler)
 *   http://localhost:7070/nonexistent   → 404 custom error page (error handler)
 */
@SuppressWarnings("deprecation")
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
            config.registerPlugin(new DevReloadPlugin());
            // For custom build commands:
            // config.registerPlugin(new DevReloadPlugin(reload -> {
            //     reload.compileCommand = "mvn compile -o -q --batch-mode";
            // }));

        }).start(7070);
    }

}
