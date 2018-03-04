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
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import static io.javalin.ApiBuilder.*;

@BenchmarkOptions(benchmarkRounds = 10000, warmupRounds = 500, concurrency = 4, clock = Clock.NANO_TIME)
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
    public static void setup() throws IOException {
        app = Javalin.create()
                .port(0)
                .routes(() -> {
                    before(ctx -> ctx.header("X-BEFORE", "Before"));
                    before(ctx -> ctx.status(200));
                    //Some CRUD API
                    path("user", () -> {
                        post(ctx -> ctx.result("Created"));
                        path(":userId", () -> {
                            get(ctx -> ctx.result("Get user " + ctx.param("userId")));
                            patch(ctx -> ctx.result("Update user " + ctx.param("userId")));
                            delete(ctx -> ctx.result("Delete user " + ctx.param("userId")));
                        });
                    });

                    path("message/:userId", () -> {
                        get(ctx -> ctx.result("Messages for " + ctx.param("userId")));
                        post(":recipientId", ctx -> ctx.result("Send from " + ctx.param("userId") + " to " + ctx.param("recipientId")));
                        path("drafts", () -> {
                            get(ctx -> ctx.result("Drafts for " + ctx.param("userId")));
                            path(":draftId", () -> {
                                get(ctx -> ctx.result("Get draft " + ctx.param("draftId")));
                                patch(ctx -> ctx.result("Update draft " + ctx.param("draftId")));
                                delete(ctx -> ctx.result("Delete draft " + ctx.param("draftId")));
                            });
                        });
                    });

                    get("redirect/*", ctx -> ctx.redirect(ctx.splat(0)));
                    post("redirect/*", ctx -> ctx.redirect(ctx.splat(0)));

                    get("health", ctx -> {});

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

        Unirest.get(origin + "/redirect/new/payment/").asString();
        Unirest.get(origin + "/redirect/old/payment/").asString();
        Unirest.post(origin + "/redirect/old/accept/").asString();

        Unirest.get(origin + "/health").asString();
    }
}
