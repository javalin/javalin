/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.performance;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.Clock;
import com.mashape.unirest.http.Unirest;
import io.javalin.Context;
import io.javalin.Javalin;
import io.javalin.apibuilder.CrudHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import static io.javalin.apibuilder.ApiBuilder.after;
import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.crud;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

@BenchmarkOptions(benchmarkRounds = 40000, warmupRounds = 5000, concurrency = 4, clock = Clock.NANO_TIME)
public class StupidPerformanceTest {

    private static Javalin app;
    private static String origin;

    @Rule
    public TestRule benchmarkRun = new BenchmarkRule();

    @AfterClass
    public static void tearDown() {
        app.stop();
    }

    @BeforeClass
    public static void setup() {
        // Thread.sleep(7500) // uncomment if running with VisualVM
        app = Javalin.create()
            .port(0)
            .routes(() -> {
                before(ctx -> ctx.header("X-BEFORE", "Before"));
                before(ctx -> ctx.status(200));
                get("/health", ctx -> ctx.result("OK"));
                crud("/users/:user-id", new GenericController());
                path("/nested/path", () -> {
                    crud("/messages/:message-id", new GenericController());
                });
                after(ctx -> ctx.header("X-AFTER", "After"));
            }).start();
        origin = "http://localhost:" + app.port();
    }

    @Test
    @Ignore("Just for running manually")
    public void testPerformanceMaybe() throws Exception {

        Unirest.get(origin + "/health").asString();
        Unirest.get(origin + "/health").asString();

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

        Unirest.get(origin + "/not-found").asString(); // slow because WebJars

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
