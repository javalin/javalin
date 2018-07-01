/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.HttpMethod;
import io.javalin.util.BaseTest;
import org.junit.Test;
import static io.javalin.ApiBuilder.after;
import static io.javalin.ApiBuilder.before;
import static io.javalin.ApiBuilder.delete;
import static io.javalin.ApiBuilder.get;
import static io.javalin.ApiBuilder.patch;
import static io.javalin.ApiBuilder.path;
import static io.javalin.ApiBuilder.post;
import static io.javalin.ApiBuilder.put;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestApiBuilder extends BaseTest {

    @Test
    public void autoPrefix_path_works() throws Exception {
        app.routes(() -> {
            path("level-1", () -> {
                get("/hello", simpleAnswer("Hello from level 1"));
            });
        });
        assertThat(http.getBody("/level-1/hello"), is("Hello from level 1"));
    }

    @Test
    public void routesWithoutPathArg_works() throws Exception {
        app.routes(() -> {
            path("api", () -> {
                get(okHandler);
                post(okHandler);
                put(okHandler);
                delete(okHandler);
                patch(okHandler);
                path("user", () -> {
                    get(okHandler);
                    post(okHandler);
                    put(okHandler);
                    delete(okHandler);
                    patch(okHandler);
                });
            });
        });
        HttpMethod[] httpMethods = new HttpMethod[]{HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.PATCH};
        for (HttpMethod httpMethod : httpMethods) {
            assertThat(http.call(httpMethod, "/api").getStatus(), is(200));
            assertThat(http.call(httpMethod, "/api/user").getStatus(), is(200));
        }
    }

    @Test
    public void test_pathWorks_forGet() throws Exception {
        app.routes(() -> {
            get("/hello", simpleAnswer("Hello from level 0"));
            path("/level-1", () -> {
                get("/hello", simpleAnswer("Hello from level 1"));
                get("/hello-2", simpleAnswer("Hello again from level 1"));
                post("/create-1", simpleAnswer("Created something at level 1"));
                path("/level-2", () -> {
                    get("/hello", simpleAnswer("Hello from level 2"));
                    path("/level-3", () -> {
                        get("/hello", simpleAnswer("Hello from level 3"));
                    });
                });
            });
        });
        assertThat(http.getBody("/hello"), is("Hello from level 0"));
        assertThat(http.getBody("/level-1/hello"), is("Hello from level 1"));
        assertThat(http.getBody("/level-1/level-2/hello"), is("Hello from level 2"));
        assertThat(http.getBody("/level-1/level-2/level-3/hello"), is("Hello from level 3"));
    }

    private Handler simpleAnswer(String body) {
        return ctx -> ctx.result(body);
    }

    @Test
    public void test_pathWorks_forFilters() throws Exception {
        app.routes(() -> {
            path("level-1", () -> {
                before(ctx -> ctx.result("1"));
                path("level-2", () -> {
                    path("level-3", () -> {
                        get("/hello", updateAnswer("Hello"));
                    });
                    after(updateAnswer("2"));
                });
            });
        });
        assertThat(http.getBody("/level-1/level-2/level-3/hello"), is("1Hello2"));
    }

    @Test
    public void test_pathWorks_forNonSlashVerb() throws Exception {
        app.routes(() -> {
            path("level-1", () -> {
                get(ctx -> ctx.result("level-1"));
                get("hello", ctx -> ctx.result("Hello"));
            });
        });
        assertThat(http.getBody("/level-1"), is("level-1"));
        assertThat(http.getBody("/level-1/hello"), is("Hello"));
    }

    private Handler updateAnswer(String body) {
        return ctx -> ctx.result(ctx.resultString() + body);
    }

    @Test(expected = IllegalStateException.class)
    public void test_throwsException_ifUsedOutsideRoutes() {
        ApiBuilder.get("/", ctx -> ctx.result(""));
    }

}

