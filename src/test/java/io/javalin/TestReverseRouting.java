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
    public void test_pathFinder_works_basic() {
        app.get("/hello-get", helloHandler);
        assertThat(app.pathFinder(helloHandler), is("/hello-get"));
    }

    @Test
    public void test_pathFinder_returnsNullForUnmappedHandler() {
        assertThat(app.pathFinder(helloHandler), is(nullValue()));
    }

    @Test
    public void test_pathFinder_works_typed() {
        app.get("/hello-get", helloHandler);
        app.post("/hello-post", helloHandler);
        app.before("/hello-post", helloHandler);
        assertThat(app.pathFinder(helloHandler), is("/hello-get"));
        assertThat(app.pathFinder(helloHandler, HandlerType.POST), is("/hello-post"));
        assertThat(app.pathFinder(helloHandler, HandlerType.BEFORE), is("/hello-post"));
    }

    @Test
    public void test_pathFinder_findsFirstForMultipleUsages() {
        app.get("/hello-1", helloHandler);
        app.get("/hello-2", helloHandler);
        app.get("/hello-3", helloHandler);
        assertThat(app.pathFinder(helloHandler), is("/hello-1"));
    }

}
