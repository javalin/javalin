/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.io.IOException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestMultipleInstances {

    private static Javalin app1;
    private static Javalin app2;
    private static Javalin app3;

    @BeforeClass
    public static void setup() {
        app1 = Javalin.create().start(0);
        app2 = Javalin.create().start(0);
        app3 = Javalin.create().start(0);
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
        assertThat(getBody(app1.port(), "/hello-1"), is("Hello first World"));
        assertThat(getBody(app2.port(), "/hello-2"), is("Hello second World"));
        assertThat(getBody(app3.port(), "/hello-3"), is("Hello third World"));
    }

    static String getBody(int port, String pathname) throws UnirestException {
        return Unirest.get("http://localhost:" + port + pathname).asString().getBody();
    }

}
