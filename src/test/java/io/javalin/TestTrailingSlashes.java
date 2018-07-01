/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.newutil.TestUtil;
import org.junit.Test;
import static io.javalin.ApiBuilder.get;
import static io.javalin.ApiBuilder.path;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestTrailingSlashes {

    @Test
    public void test_dontIgnore_works() {
        new TestUtil(Javalin.create().dontIgnoreTrailingSlashes()).test((app, http) -> {
            app.get("/hello", ctx -> ctx.result("Hello, slash!"));
            assertThat(http.getBody("/hello"), is("Hello, slash!"));
            assertThat(http.getBody("/hello/"), is("Not found"));
        });
    }

    @Test
    public void test_ignore_works() {
        new TestUtil().test((app, http) -> {
            app.get("/hello", ctx -> ctx.result("Hello, slash!"));
            assertThat(http.getBody("/hello"), is("Hello, slash!"));
            assertThat(http.getBody("/hello/"), is("Hello, slash!"));
        });
    }

    @Test
    public void test_dontIgnore_works_apiBuilder() {
        new TestUtil(Javalin.create().dontIgnoreTrailingSlashes()).test((app, http) -> {
            app.routes(() -> {
                path("a", () -> {
                    get(ctx -> ctx.result("a"));
                    get("/", ctx -> ctx.result("a-slash"));
                });
            });
            assertThat(http.getBody("/a"), is("a"));
            assertThat(http.getBody("/a/"), is("a-slash"));
        });
    }

    @Test
    public void test_ignore_works_apiBuilder() {
        new TestUtil().test((app, http) -> {
            app.routes(() -> {
                path("a", () -> {
                    get(ctx -> ctx.result("a"));
                    get("/", ctx -> ctx.result("a-slash"));
                });
            });
            assertThat(http.getBody("/a"), is("a"));
            assertThat(http.getBody("/a/"), is("a"));
        });
    }

}
