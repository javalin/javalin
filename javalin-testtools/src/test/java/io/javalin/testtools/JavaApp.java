package io.javalin.testtools;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.sse.SseClient;

import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.sse;

// We're using statics for simplicity's sake, but you could
// make it non static and do dependency injection or whatever
public class JavaApp {

    public static Javalin app = Javalin.create(javalin -> {
        javalin.ignoreTrailingSlashes = false;
    }).routes(() -> {
        get("/hello", HelloController::hello);
    });

    static class HelloController {
        public static void hello(Context ctx) {
            ctx.result("Hello, app!");
        }

    }

}
