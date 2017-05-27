/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestApiBuilder extends _UnirestBaseTest {

    @Test
    public void test_pathWorks_forGet() throws Exception {
        app.routes(() -> {
            ApiBuilder.get("/hello", simpleAnswer("Hello from level 0"));
            ApiBuilder.path("/level-1", () -> {
                ApiBuilder.get("/hello", simpleAnswer("Hello from level 1"));
                ApiBuilder.get("/hello-2", simpleAnswer("Hello again from level 1"));
                ApiBuilder.post("/create-1", simpleAnswer("Created something at level 1"));
                ApiBuilder.path("/level-2", () -> {
                    ApiBuilder.get("/hello", simpleAnswer("Hello from level 2"));
                    ApiBuilder.path("/level-3", () -> {
                        ApiBuilder.get("/hello", simpleAnswer("Hello from level 3"));
                    });
                });
            });
        });
        assertThat(GET_body("/hello"), is("Hello from level 0"));
        assertThat(GET_body("/level-1/hello"), is("Hello from level 1"));
        assertThat(GET_body("/level-1/level-2/hello"), is("Hello from level 2"));
        assertThat(GET_body("/level-1/level-2/level-3/hello"), is("Hello from level 3"));
    }

    private Handler simpleAnswer(String body) {
        return (req, res) -> res.body(body);
    }

    @Test
    public void test_pathWorks_forFilters() throws Exception {
        app.routes(() -> {
            ApiBuilder.path("/level-1", () -> {
                ApiBuilder.before("/*", (req, res) -> res.body("1"));
                ApiBuilder.path("/level-2", () -> {
                    ApiBuilder.path("/level-3", () -> {
                        ApiBuilder.get("/hello", updateAnswer("Hello"));
                    });
                    ApiBuilder.after("/*", updateAnswer("2"));
                });
            });
        });
        assertThat(GET_body("/level-1/level-2/level-3/hello"), is("1Hello2"));
    }

    private Handler updateAnswer(String body) {
        return (req, res) -> res.body(res.body() + body);
    }

}

