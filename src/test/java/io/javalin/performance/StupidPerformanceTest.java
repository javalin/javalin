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
import static io.javalin.ApiBuilder.after;
import static io.javalin.ApiBuilder.before;
import static io.javalin.ApiBuilder.get;
import static io.javalin.ApiBuilder.path;
import static io.javalin.ApiBuilder.post;

@BenchmarkOptions(callgc = false, benchmarkRounds = 10000, warmupRounds = 500, concurrency = 4, clock = Clock.NANO_TIME)
public class StupidPerformanceTest {

    @Rule
    public TestRule benchmarkRun = new BenchmarkRule();

    private static Javalin app;

    @AfterClass
    public static void tearDown() {
        app.stop();
    }

    @BeforeClass
    public static void setup() throws IOException {
        app = Javalin.create()
            .port(7000)
            .routes(() -> {
                before(ctx -> ctx.status(123));
                before(ctx -> ctx.status(200));
                get("/hello", ctx -> ctx.result("Hello from level 0"));
                path("/level-1", () -> {
                    get("/hello", ctx -> ctx.result("Hello from level 1"));
                    get("/hello-2", ctx -> ctx.result("Hello again from level 1"));
                    get("/param/:param", ctx -> {
                        ctx.result(ctx.param("param"));
                    });
                    get("/queryparam", ctx -> {
                        ctx.result(ctx.queryParam("queryparam"));
                    });
                    post("/create-1", ctx -> ctx.result("Created something at level 1"));
                    path("/level-2", () -> {
                        get("/hello", ctx -> ctx.result("Hello from level 2"));
                        path("/level-3", () -> {
                            get("/hello", ctx -> ctx.result("Hello from level 3"));
                        });
                    });
                });
                after(ctx -> ctx.header("X-AFTER", "After"));
            }).start();
    }

    @Test
    @Ignore("Just for running manually")
    public void testPerformanceMaybe() throws Exception {
        Unirest.get("http://localhost:7000/param/test").asString();
        Unirest.get("http://localhost:7000/queryparam/").queryString("queryparam", "value").asString();
        Unirest.get("http://localhost:7000/level-1/level-2/level-3/hello").asString();
        Unirest.get("http://localhost:7000/level-1/level-2/level-3/hello").asString();
    }

}
