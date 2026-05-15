package devreloadtest;

import io.javalin.http.Context;

/**
 * Simple controller used by DevReloadPlugin tests.
 */
public class DevReloadController {
    public static void handle(Context ctx) {
        ctx.result("original");
    }
}
