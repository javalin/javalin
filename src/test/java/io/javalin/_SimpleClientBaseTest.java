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

import io.javalin.util.SimpleHttpClient;

public class _SimpleClientBaseTest {

    static Javalin app;
    static String origin = "http://localhost:7777";

    static SimpleHttpClient simpleHttpClient;

    @BeforeClass
    public static void setup() throws IOException {
        app = Javalin.Companion.create()
            .port(7777)
            .start()
            .awaitInitialization();
        simpleHttpClient = new SimpleHttpClient();
    }

    @After
    public void clearRoutes() {
        app.clearInternalMappers();
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
        app.awaitTermination();
    }
}
