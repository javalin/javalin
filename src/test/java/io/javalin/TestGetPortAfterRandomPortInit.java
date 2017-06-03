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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestGetPortAfterRandomPortInit {

    private static Javalin app;
    private static int port;

    @BeforeClass
    public static void setup() throws IOException {
        app = Javalin.Companion.create()
            .port(0)
            .start()
            .awaitInitialization();
        port = app.port();
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
    }

    @Test
    public void test_get_helloWorld() throws Exception {
        app.get("/hello", (req, res) -> res.body("Hello World"));
        assertThat(Unirest.get("http://localhost:" + port + "/hello").asString().getStatus(), is(200));
    }

}
