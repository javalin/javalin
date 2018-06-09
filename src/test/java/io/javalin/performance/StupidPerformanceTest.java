/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.performance;

import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.Clock;
import com.mashape.unirest.http.Unirest;
import io.javalin.Javalin;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import static io.javalin.ApiBuilder.after;
import static io.javalin.ApiBuilder.before;
import static io.javalin.ApiBuilder.delete;
import static io.javalin.ApiBuilder.get;
import static io.javalin.ApiBuilder.patch;
import static io.javalin.ApiBuilder.path;
import static io.javalin.ApiBuilder.post;

@BenchmarkOptions(benchmarkRounds = 40000, warmupRounds = 5000, concurrency = 4, clock = Clock.NANO_TIME)
public class StupidPerformanceTest {

    @Rule
    public TestRule benchmarkRun = new BenchmarkRule();

    private static Javalin app;
    private static String origin;

    @AfterClass
    public static void tearDown() {
        app.stop();
    }

    @BeforeClass
    public static void setup() throws Exception {
        // Thread.sleep(7500) // uncomment if running with VisualVM
        app = Javalin.create()
            .port(0)
            .routes(() -> {
                before(ctx -> ctx.header("X-BEFORE", "Before"));
                before(ctx -> ctx.status(200));
                //Some CRUD API
                path("user", () -> {
                    post(ctx -> ctx.result("Created"));
                    path(":userId", () -> {
                        get(ctx -> ctx.result("Get user " + ctx.pathParam("userId")));
                        patch(ctx -> ctx.result("Update user " + ctx.pathParam("userId")));
                        delete(ctx -> ctx.result("Delete user " + ctx.pathParam("userId")));
                    });
                });

                path("message/:userId", () -> {
                    get(ctx -> ctx.result("Messages for " + ctx.pathParam("userId")));
                    post(":recipientId", ctx -> ctx.result("Send from " + ctx.pathParam("userId") + " to " + ctx.pathParam("recipientId")));
                    path("drafts", () -> {
                        get(ctx -> ctx.result("Drafts for " + ctx.pathParam("userId")));
                        path(":draftId", () -> {
                            get(ctx -> ctx.result("Get draft " + ctx.pathParam("draftId")));
                            patch(ctx -> ctx.result("Update draft " + ctx.pathParam("draftId")));
                            delete(ctx -> ctx.result("Delete draft " + ctx.pathParam("draftId")));
                        });
                    });
                });

                get("health", ctx -> {
                });

                after(ctx -> ctx.header("X-AFTER", "After"));
            }).start();
        origin = "http://localhost:" + app.port();
    }

    @Test
    @Ignore("Just for running manually")
    public void testPerformanceMaybe() throws Exception {
        //User interactions
        Unirest.get(origin + "/user/1/").asString();
        Unirest.get(origin + "/user/2/").asString();
        Unirest.get(origin + "/user/3/").asString();
        Unirest.post(origin + "/user/").asString();
        Unirest.patch(origin + "/user/1/").asString();

        Unirest.get(origin + "/health/").asString();

        Unirest.get(origin + "/message/1/").asString();
        Unirest.get(origin + "/message/2/").asString();
        Unirest.get(origin + "/message/3/").asString();

        Unirest.get(origin + "/health/draft/asd/creep/").asString();

        Unirest.get(origin + "/message/1/drafts/2/").asString();
        Unirest.delete(origin + "/message/1/drafts/3/").asString();
        Unirest.patch(origin + "/message/1/drafts/2/").asString();

        Unirest.get(origin + "/health/draft/asd/creep/").asString();

        Unirest.get(origin + "/health").asString();
    }
}
