/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.Unirest;
import org.junit.Test;
import static io.javalin.ApiBuilder.get;
import static io.javalin.ApiBuilder.path;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestTrailingSlashes {

    @Test
    public void test_dontIgnore_works() throws Exception {
        Javalin app = Javalin.create()
            .port(0)
            .dontIgnoreTrailingSlashes()
            .start()
            .get("/hello", ctx -> ctx.result("Hello, slash!"));
        assertThat(getBody(app, "/hello"), is("Hello, slash!"));
        assertThat(getBody(app, "/hello/"), is("Not found"));
        app.stop();
    }

    @Test
    public void test_ignore_works() throws Exception {
        Javalin app = Javalin.create()
            .port(0)
            .start()
            .get("/hello", ctx -> ctx.result("Hello, slash!"));
        assertThat(getBody(app, "/hello"), is("Hello, slash!"));
        assertThat(getBody(app, "/hello/"), is("Hello, slash!"));
        app.stop();
    }

    @Test
    public void test_dontIgnore_works_apiBuilder() throws Exception {
        Javalin app = Javalin.create()
            .port(0)
            .dontIgnoreTrailingSlashes()
            .routes(() -> {
                path("a", () -> {
                    get(ctx -> ctx.result("a"));
                    get("/", ctx -> ctx.result("a-slash"));
                });
            })
            .start();
        assertThat(getBody(app, "/a"), is("a"));
        assertThat(getBody(app, "/a/"), is("a-slash"));
        app.stop();
    }

    @Test
    public void test_ignore_works_apiBuilder() throws Exception {
        Javalin app = Javalin.create()
            .port(0)
            .routes(() -> {
                path("a", () -> {
                    get(ctx -> ctx.result("a"));
                    get("/", ctx -> ctx.result("a-slash"));
                });
            })
            .start();
        assertThat(getBody(app, "/a"), is("a"));
        assertThat(getBody(app, "/a/"), is("a"));
        app.stop();
    }

    private String getBody(Javalin app, String path) throws Exception {
        return Unirest.get("http://localhost:" + app.port() + path).asString().getBody();
    }

}
