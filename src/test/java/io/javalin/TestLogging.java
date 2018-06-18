/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestLogging {

    @Test
    public void test_noLogging() throws Exception {
        Javalin app = Javalin.create().port(0).start();
        addAndTestEndpoints(app);
        app.stop();
    }

    @Test
    public void test_debugLogging() throws Exception {
        Javalin app = Javalin.create().port(0).enableDebugRequestLogs().start();
        addAndTestEndpoints(app);
        app.stop();
    }

    @Test
    public void test_customLogger() throws Exception {
        Javalin app = Javalin.create().port(0).requestLogger((ctx, executionTimeMs) -> {
            System.out.println("That took " + executionTimeMs + " milliseconds");
        }).start();
        addAndTestEndpoints(app);
        app.stop();
    }

    private static void addAndTestEndpoints(Javalin app) throws UnirestException {
        app.get("/blocking", ctx -> ctx.result("Hello Blocking World!"));
        app.get("/async", ctx -> {
            CompletableFuture<String> future = new CompletableFuture<>();
            Executors.newSingleThreadScheduledExecutor().schedule(() -> future.complete("Hello Async World!"), 10, TimeUnit.MILLISECONDS);
            ctx.result(future);
        });
        HttpResponse<String> response = Unirest.get("http://localhost:" + app.port() + "/async").asString();
        assertThat(response.getBody(), is("Hello Async World!"));
        HttpResponse<String> response2 = Unirest.get("http://localhost:" + app.port() + "/blocking").asString();
        assertThat(response2.getBody(), is("Hello Blocking World!"));
    }

}
