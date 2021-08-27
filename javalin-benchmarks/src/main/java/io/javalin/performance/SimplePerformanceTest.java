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
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

import static io.javalin.apibuilder.ApiBuilder.after;
import static io.javalin.apibuilder.ApiBuilder.before;
import static io.javalin.apibuilder.ApiBuilder.crud;
import static io.javalin.apibuilder.ApiBuilder.get;
import static io.javalin.apibuilder.ApiBuilder.path;

public class SimplePerformanceTest {

    @State(Scope.Benchmark)
    public static class JavalinState {

        public String origin;
        public Javalin app = Javalin.create(
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

        @Setup(Level.Trial)
        public void setup() {
            // Thread.sleep(7500) // uncomment if running with VisualVM
            app.start(0);
            origin = "http://localhost:" + app.port();
        }

        @TearDown(Level.Trial)
        public void tearDown() {
            app.stop();
        }
    }

    @Warmup(iterations = 5)
    @Measurement(iterations = 15)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Fork(4)
    @Benchmark
    public void testPerformanceMaybe(JavalinState state, Blackhole blackhole) throws Exception {
        Unirest.get(state.origin + "/health").asString();

        Unirest.get(state.origin + "/my-path/my-param/123").asString();
        Unirest.get(state.origin + "/1234/1/2/3/4").asString();

        Unirest.get(state.origin + "/users").asString();
        Unirest.post(state.origin + "/users").asString();
        Unirest.get(state.origin + "/users/1").asString();
        Unirest.patch(state.origin + "/users/1").asString();
        Unirest.delete(state.origin + "/users/1").asString();

        Unirest.get(state.origin + "/nested/path/messages").asString();
        Unirest.post(state.origin + "/nested/path/messages").asString();
        Unirest.get(state.origin + "/nested/path/messages/1").asString();
        Unirest.patch(state.origin + "/nested/path/messages/1").asString();
        Unirest.delete(state.origin + "/nested/path/messages/1").asString();

        blackhole.consume(Unirest.get(state.origin + "/not-found").asString());
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
