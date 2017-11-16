/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.core.HandlerType;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TestReverseRouting extends _UnirestBaseTest {

    private Handler helloHandler = ctx -> ctx.result("Hello World");

    @Test
    public void test_pathFinder_works_basic() throws Exception {
        app.get("/hello-get", helloHandler);
        assertThat(app.pathFinder(helloHandler), is("/hello-get"));
    }

    @Test
    public void test_pathFinder_returnsNullForUnmappedHandler() throws Exception {
        assertThat(app.pathFinder(helloHandler), is(nullValue()));
    }

    @Test
    public void test_pathFinder_works_typed() throws Exception {
        app.get("/hello-get", helloHandler);
        app.post("/hello-post", helloHandler);
        app.before("/hello-post", helloHandler);
        assertThat(app.pathFinder(helloHandler), is("/hello-get"));
        assertThat(app.pathFinder(helloHandler, HandlerType.POST), is("/hello-post"));
        assertThat(app.pathFinder(helloHandler, HandlerType.BEFORE), is("/hello-post"));
    }

    @Test
    public void test_pathFinder_findsFirstForMultipleUsages() throws Exception {
        app.get("/hello-1", helloHandler);
        app.get("/hello-2", helloHandler);
        app.get("/hello-3", helloHandler);
        assertThat(app.pathFinder(helloHandler), is("/hello-1"));
    }

}
