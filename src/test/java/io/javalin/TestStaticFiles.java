/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;


public class TestStaticFiles {

    private static Javalin app;
    private static String origin = "http://localhost:7777";

    @BeforeClass
    public static void setup() throws IOException {
        app = Javalin.create()
            .port(7777)
            .enableStaticFiles("/public")
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
        HttpResponse<String> response = Unirest.get(origin + "/html.html").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), containsString("HTML works"));

    }

    @Test
    public void test_getJs() throws Exception {
        HttpResponse<String> response = Unirest.get(origin + "/script.js").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), containsString("JavaScript works"));
    }

    @Test
    public void test_getCss() throws Exception {
        HttpResponse<String> response = Unirest.get(origin + "/styles.css").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), containsString("CSS works"));
    }

    @Test
    public void test_beforeFilter() throws Exception {
        app.before("/protected/*", ctx -> {
            throw new HaltException(401, "Protected");
        });
        HttpResponse<String> response = Unirest.get(origin + "/protected/secret.html").asString();
        assertThat(response.getStatus(), is(401));
        assertThat(response.getBody(), is("Protected"));
    }

    @Test
    public void test_rootReturns404_ifNoWelcomeFile() throws Exception {
        HttpResponse<String> response = Unirest.get(origin + "/").asString();
        assertThat(response.getStatus(), is(404));
        assertThat(response.getBody(), is("Not found"));
    }

    @Test
    public void test_rootReturnsWelcomeFile_ifWelcomeFileExists() throws Exception {
        HttpResponse<String> response = Unirest.get(origin + "/subdir/").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("<h1>Welcome file</h1>"));
    }

}
