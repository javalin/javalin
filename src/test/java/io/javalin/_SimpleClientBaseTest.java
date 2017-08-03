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
        app = Javalin.create()
            .setPort(7777)
            .start();
        simpleHttpClient = new SimpleHttpClient();
    }

    @After
    public void clearRoutes() {
        app.pathMatcher.getHandlerEntries().clear();
        app.errorMapper.getErrorHandlerMap().clear();
        app.exceptionMapper.getExceptionMap().clear();
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
    }
}
