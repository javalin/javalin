/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.javalin.embeddedserver.Location;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;


public class TestStaticFiles_external {

    private static Javalin app;

    @BeforeClass
    public static void setup() throws IOException {
        app = Javalin.create()
            .port(7777)
            .enableStaticFiles("src/test/external/", Location.EXTERNAL)
            .start()
            .awaitInitialization();
    }

    @After
    public void clearRoutes() {
        app.pathMatcher.clear();
        app.errorMapper.clear();
        app.exceptionMapper.clear();
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
        app.awaitTermination();
    }

    @Test
    public void test_Html() throws Exception {
        String origin = "http://localhost:7777";
        HttpResponse<String> response = Unirest.get(origin + "/html.html").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), containsString("HTML works"));

    }

}
