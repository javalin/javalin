/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.newutil.TestUtil;
import java.net.URLEncoder;
import org.junit.Test;
import static io.javalin.ApiBuilder.get;
import static io.javalin.ApiBuilder.path;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class TestRouting {

    @Test
    public void test_aBunchOfRoutes() {
        new TestUtil().test((app, http) -> {
            app.get("/", ctx -> ctx.result("/"));
            app.get("/path", ctx -> ctx.result("/path"));
            app.get("/path/:path-param", ctx -> ctx.result("/path/" + ctx.pathParam("path-param")));
            app.get("/path/:path-param/*", ctx -> ctx.result("/path/" + ctx.pathParam("path-param") + "/" + ctx.splat(0)));
            app.get("/*/*", ctx -> ctx.result("/" + ctx.splat(0) + "/" + ctx.splat(1)));
            app.get("/*/unreachable", ctx -> ctx.result("reached"));
            app.get("/*/*/:path-param", ctx -> ctx.result("/" + ctx.splat(0) + "/" + ctx.splat(1) + "/" + ctx.pathParam("path-param")));
            app.get("/*/*/:path-param/*", ctx -> ctx.result("/" + ctx.splat(0) + "/" + ctx.splat(1) + "/" + ctx.pathParam("path-param") + "/" + ctx.splat(2)));
            assertThat(http.getBody("/"), is("/"));
            assertThat(http.getBody("/path"), is("/path"));
            assertThat(http.getBody("/path/p"), is("/path/p"));
            assertThat(http.getBody("/path/p/s"), is("/path/p/s"));
            assertThat(http.getBody("/s1/s2"), is("/s1/s2"));
            assertThat(http.getBody("/s/unreachable"), not("reached"));
            assertThat(http.getBody("/s1/s2/p"), is("/s1/s2/p"));
            assertThat(http.getBody("/s1/s2/p/s3"), is("/s1/s2/p/s3"));
            assertThat(http.getBody("/s/s/s/s"), is("/s/s/s/s"));
        });
    }

    @Test
    public void test_paramAndSplat() {
        new TestUtil().test((app, http) -> {
            app.get("/:path-param/path/*", ctx -> ctx.result(ctx.pathParam("path-param") + ctx.splat(0)));
            assertThat(http.getBody("/path-param/path/splat"), is("path-paramsplat"));
        });
    }

    @Test
    public void test_encodedParam() {
        new TestUtil().test((app, http) -> {
            app.get("/:path-param", ctx -> ctx.result(ctx.pathParam("path-param")));
            String paramValue = "te/st";
            assertThat(http.getBody("/" + URLEncoder.encode(paramValue, "UTF-8")), is(paramValue));
        });
    }

    @Test
    public void test_encdedParamAndEncodedSplat() {
        new TestUtil().test((app, http) -> {
            app.get("/:path-param/path/*", ctx -> ctx.result(ctx.pathParam("path-param") + ctx.splat(0)));
            String responseBody = http.getBody("/"
                + URLEncoder.encode("java/kotlin", "UTF-8")
                + "/path/"
                + URLEncoder.encode("/java/kotlin", "UTF-8")
            );
            assertThat(responseBody, is("java/kotlin/java/kotlin"));
        });
    }

    @Test
    public void test_caseSensitive_path() {
        new TestUtil().test((app, http) -> {
            app.get("/HELLO", ctx -> ctx.result("Hello"));
            assertThat(http.getBody("/hello"), is("Hello"));
        });
    }

    @Test
    public void test_caseSensitive_paramName_isLowercased() {
        new TestUtil().test((app, http) -> {
            app.get("/:ParaM", ctx -> ctx.result(ctx.pathParam("pArAm")));
            assertThat(http.getBody("/path-param"), is("path-param"));
        });
    }

    @Test
    public void test_caseSensitive_paramValue_isLowerCased() {
        new TestUtil().test((app, http) -> {
            app.get("/:path-param", ctx -> ctx.result(ctx.pathParam("path-param")));
            assertThat(http.getBody("/SomeCamelCasedValue"), is("somecamelcasedvalue"));
        });
    }

    @Test
    public void test_regex_path() {
        new TestUtil().test((app, http) -> {
            app.get("/:path-param/[0-9]+/", ctx -> ctx.result(ctx.pathParam("path-param")));
            assertThat(http.getBody("/test/pathParam"), is("Not found"));
            assertThat(http.getBody("/test/21"), is("test"));
        });
    }

    @Test
    public void test_trailing_slashes_and_params() {
        new TestUtil().test((app, http) -> {
            app.routes(() -> {
                path("test", () -> {
                    path(":id", () -> {
                        get(ctx -> ctx.result(ctx.pathParam("id")));
                    });
                    get(ctx -> ctx.result("test"));
                });
            });
            assertThat(http.getBody("/test/path-param/"), is("path-param"));
            assertThat(http.getBody("/test/"), is("test"));
        });
    }

}
