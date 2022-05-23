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
        sse("/listen", HelloController::sayHelloFiveTimes);
    });

    static class HelloController {
        public static void hello(Context ctx) {
            ctx.result("Hello, app!");
        }

        public static void sayHelloFiveTimes(SseClient sseClient) {
            try {
                for (int i = 0; i < 5; i++) {
                    sseClient.sendEvent("Hello!");
                    Thread.sleep(200);
                }
                sseClient.close();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
