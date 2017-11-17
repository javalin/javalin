/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.util.SimpleHttpClient;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class _SimpleClientBaseTest {

    static Javalin app;
    static String origin = null;

    static SimpleHttpClient simpleHttpClient;

    @BeforeClass
    public static void setup() throws IOException {
        app = Javalin.create()
            .port(0)
            .start();
        simpleHttpClient = new SimpleHttpClient();
        origin =  "http://localhost:" + app.port();
    }

    @After
    public void clear() {
        app.clearMatcherAndMappers();
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
    }
}
