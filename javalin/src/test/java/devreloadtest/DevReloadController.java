package devreloadtest;

import io.javalin.http.Context;

/**
 * Simple controller used by DevReloadPlugin end-to-end tests.
 * The test modifies and restores this file to verify that the plugin
 * detects the change, recompiles, and reloads it via a fresh classloader.
 *
 * This class is intentionally NOT in the io.javalin package so that the
 * fresh classloader picks it up (io.javalin.* is delegated to parent).
 */
public class DevReloadController {
    public static void handle(Context ctx) {
        ctx.result("original");
    }
}

