/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.core.util.Util;
import io.javalin.util.TestUtil;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class TestContextPath {

    static Javalin app;

    @Test
    public void test_normalizeContextPath_works() {
        Function<String, String> normalize = Util.INSTANCE::normalizeContextPath;
        assertThat(normalize.apply("path"), is("/path"));
        assertThat(normalize.apply("/path"), is("/path"));
        assertThat(normalize.apply("/path/"), is("/path"));
        assertThat(normalize.apply("//path/"), is("/path"));
        assertThat(normalize.apply("/path//"), is("/path"));
        assertThat(normalize.apply("////path////"), is("/path"));
    }

    @Test
    public void test_prefixPath_works() {
        BiFunction<String, String, String> prefix = Util.INSTANCE::prefixContextPath;
        assertThat(prefix.apply("/c-p", "*"), is("*"));
        assertThat(prefix.apply("/c-p", "/*"), is("/c-p/*"));
        assertThat(prefix.apply("/c-p", "path"), is("/c-p/path"));
        assertThat(prefix.apply("/c-p", "/path"), is("/c-p/path"));
        assertThat(prefix.apply("/c-p", "//path"), is("/c-p/path"));
        assertThat(prefix.apply("/c-p", "/path/"), is("/c-p/path/"));
        assertThat(prefix.apply("/c-p", "//path//"), is("/c-p/path/"));
    }

    @Test
    public void test_router_works() {
        new TestUtil(Javalin.create().contextPath("/context-path")).test((app, http) -> {
            app.get("/hello", ctx -> ctx.result("Hello World"));
            assertThat(http.getBody("/hello"), is("Not found. Request is below context-path (context-path: '/context-path')"));
            assertThat(http.getBody("/context-path/hello"), is("Hello World"));
        });
    }

    @Test
    public void test_twoLevelContextPath_works() {
        new TestUtil(Javalin.create().contextPath("/context-path/path-context")).test((app, http) -> {
            app.get("/hello", ctx -> ctx.result("Hello World"));
            assertThat(http.get("/context-path/").code(), is(404));
            assertThat(http.getBody("/context-path/path-context/hello"), is("Hello World"));
        });
    }

    @Test
    public void test_staticFiles_work() {
        new TestUtil(Javalin.create().contextPath("/context-path").enableStaticFiles("/public")).test((app, http) -> {
            assertThat(http.get("/script.js").code(), is(404));
            assertThat(http.getBody("/context-path/script.js"), containsString("JavaScript works"));
        });
    }

    @Test
    public void test_welcomeFile_works() {
        new TestUtil(Javalin.create().contextPath("/context-path").enableStaticFiles("/public")).test((app, http) -> {
            assertThat(http.getBody("/context-path/subdir/"), is("<h1>Welcome file</h1>"));
        });
    }

}
