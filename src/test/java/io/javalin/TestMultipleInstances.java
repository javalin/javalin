/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestMultipleInstances {

    private static Javalin app1;
    private static Javalin app2;
    private static Javalin app3;

    @BeforeClass
    public static void setup() throws IOException {
        app1 = Javalin.create().setPort(7001).start();
        app2 = Javalin.create().setPort(7002).start();
        app3 = Javalin.create().setPort(7003).start();
    }

    @AfterClass
    public static void tearDown() {
        app1.stop();
        app2.stop();
        app3.stop();
    }

    @Test
    public void test_getMultiple() throws Exception {
        app1.get("/hello-1", ctx -> ctx.result("Hello first World"));
        app2.get("/hello-2", ctx -> ctx.result("Hello second World"));
        app3.get("/hello-3", ctx -> ctx.result("Hello third World"));
        assertThat(getBody("7001", "/hello-1"), is("Hello first World"));
        assertThat(getBody("7002", "/hello-2"), is("Hello second World"));
        assertThat(getBody("7003", "/hello-3"), is("Hello third World"));
    }

    static String getBody(String port, String pathname) throws UnirestException {
        return Unirest.get("http://localhost:" + port + pathname).asString().getBody();
    }

}
