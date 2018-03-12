/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.javalin.ApiBuilder.get;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestApiBuilderTwoServices {

    final static int SIZE = 10;

    @Test
    public void testApiBuilder_twoServices() throws Exception {
        Javalin app1 = Javalin.create().port(0).start();
        Javalin app2 = Javalin.create().port(0).start();
        app1.routes(() -> {
            get("/hello1", ctx -> ctx.result("Hello1"));
        });
        app2.routes(() -> {
            get("/hello1", ctx -> ctx.result("Hello1"));
        });
        app1.routes(() -> {
            get("/hello2", ctx -> ctx.result("Hello2"));
        });
        app2.routes(() -> {
            get("/hello2", ctx -> ctx.result("Hello2"));
        });
        assertThat(call(app1.port(), "/hello1"), is("Hello1"));
        assertThat(call(app2.port(), "/hello1"), is("Hello1"));
        assertThat(call(app1.port(), "/hello2"), is("Hello2"));
        assertThat(call(app2.port(), "/hello2"), is("Hello2"));
        app1.stop();
        app2.stop();
    }

    @Test
    public void testApiBuilder_twoServices_async() throws Exception {
        ExecutorService ec = Executors.newFixedThreadPool(2);

        Javalin app1 = Javalin.create().port(0).start();
        Javalin app2 = Javalin.create().port(0).start();

        Future<?> f1 = ec.submit(appTest(app1));
        Future<?> f2 = ec.submit(appTest(app2));

        f1.get();
        f2.get();

        ec.shutdownNow();

        app1.stop();
        app2.stop();
    }

    private Runnable appTest(Javalin app){
        return () -> {
            try {
                createRoutes(app);
                checkRoutes(app);
            } catch (UnirestException e) {
                throw new RuntimeException("Unirest error", e);
            }
        };
    }

    private void createRoutes(final Javalin app) {
        for (int i = 0; i < SIZE; i++) {
            final int index = i;
            app.routes(() -> get("/hello" + index, ctx -> ctx.result("Hello" + index)));
        }
    }

    private void checkRoutes(final Javalin app) throws UnirestException {
        for (int i = 0; i < SIZE; i++) {
            assertThat(call(app.port(), "/hello" + i), is("Hello" + i));
        }
    }

    private String call(int port, String path) throws UnirestException {
        return Unirest.get("http://localhost:" + port + path).asString().getBody();
    }

}
