/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.performance;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import io.javalin.Handler;
import io.javalin.Javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import static io.javalin.ApiBuilder.*;

public class StupidPerformanceTest {

    private static Javalin app;

    @AfterClass
    public static void tearDown() {
        app.stop();
    }

    @BeforeClass
    public static void setup() throws IOException {
        app = Javalin.Companion.create()
            .port(7000)
            .routes(() -> {
                before((req, res) -> res.status(123));
                before((req, res) -> res.status(200));
                get("/hello", simpleAnswer("Hello from level 0"));
                path("/level-1", () -> {
                    get("/hello", simpleAnswer("Hello from level 1"));
                    get("/hello-2", simpleAnswer("Hello again from level 1"));
                    get("/param/:param", (req, res) -> {
                        res.body(req.param("param"));
                    });
                    get("/queryparam", (req, res) -> {
                        res.body(req.queryParam("queryparam"));
                    });
                    post("/create-1", simpleAnswer("Created something at level 1"));
                    path("/level-2", () -> {
                        get("/hello", simpleAnswer("Hello from level 2"));
                        path("/level-3", () -> {
                            get("/hello", simpleAnswer("Hello from level 3"));
                        });
                    });
                });
                after((req, res) -> res.header("X-AFTER", "After"));
            });
    }

    private static Handler simpleAnswer(String body) {
        return (req, res) -> res.body(body);
    }

    @Test
    @Ignore("Just for running manually")
    public void testPerformanceMaybe() throws Exception {

        long startTime = System.currentTimeMillis();
        HttpResponse<String> response;
        for (int i = 0; i < 1000; i++) {
            response = Unirest.get("http://localhost:7000/param/test").asString();
            response = Unirest.get("http://localhost:7000/queryparam/").queryString("queryparam", "value").asString();
            response = Unirest.get("http://localhost:7000/level-1/level-2/level-3/hello").asString();
            response = Unirest.get("http://localhost:7000/level-1/level-2/level-3/hello").asString();
        }
        System.out.println("took " + (System.currentTimeMillis() - startTime) + " milliseconds");
    }

}
