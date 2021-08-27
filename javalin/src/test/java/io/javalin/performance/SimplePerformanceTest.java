/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.performance;

import com.mashape.unirest.http.Unirest;
import io.javalin.Javalin;
import io.javalin.apibuilder.CrudHandler;
import io.javalin.http.Context;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

import static io.javalin.apibuilder.ApiBuilder.after;
import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.crud;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

@Warmup(iterations = 5000)
@Measurement(iterations = 35000)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(4)
public class SimplePerformanceTest {

    private static String origin;
    private static Javalin app = Javalin.create(
        config -> config.showJavalinBanner = false
    ).routes(() -> {
        before(ctx -> ctx.header("X-BEFORE", "Before"));
        before(ctx -> ctx.status(200));
        get("/my-path/{param}/*", ctx -> ctx.result(ctx.pathParam("param")));
        get("/1234/{1}/{2}/{3}/{4}", ctx -> ctx.result(ctx.pathParamMap().toString()));
        get("/health", ctx -> ctx.result("OK"));
        crud("/users/{user-id}", new GenericController());
        path("/nested/path", () -> {
            crud("/messages/{message-id}", new GenericController());
        });
        after(ctx -> ctx.header("X-AFTER", "After"));
    });

    @Setup
    public static void setup() {
        // Thread.sleep(7500) // uncomment if running with VisualVM
        app.start(0);
        origin = "http://localhost:" + app.port();
    }

    @Benchmark
    public void testPerformanceMaybe() throws Exception {

        Unirest.get(origin + "/health").asString();

        Unirest.get(origin + "/my-path/my-param/123").asString();
        Unirest.get(origin + "/1234/1/2/3/4").asString();

        Unirest.get(origin + "/users").asString();
        Unirest.post(origin + "/users").asString();
        Unirest.get(origin + "/users/1").asString();
        Unirest.patch(origin + "/users/1").asString();
        Unirest.delete(origin + "/users/1").asString();

        Unirest.get(origin + "/nested/path/messages").asString();
        Unirest.post(origin + "/nested/path/messages").asString();
        Unirest.get(origin + "/nested/path/messages/1").asString();
        Unirest.patch(origin + "/nested/path/messages/1").asString();
        Unirest.delete(origin + "/nested/path/messages/1").asString();

        Unirest.get(origin + "/not-found").asString();

    }

    @TearDown
    public static void tearDown() {
        app.stop();
    }

    static class GenericController implements CrudHandler {
        public void getAll(Context ctx) {
            ctx.result("ALL");
        }

        public void getOne(Context ctx, String resourceId) {
            ctx.result(resourceId);
        }

        public void create(Context ctx) {
            ctx.result("Created");
        }

        public void update(Context ctx, String resourceId) {
            ctx.result("Updated " + resourceId);
        }

        public void delete(Context ctx, String resourceId) {
            ctx.result("Deleted " + resourceId);
        }
    }

}
