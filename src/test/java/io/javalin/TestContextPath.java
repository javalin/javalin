/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.core.util.Util;
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
        assertThat(prefix.apply("*", "/c-p"), is("*"));
        assertThat(prefix.apply("/*", "/c-p"), is("/c-p/*"));
        assertThat(prefix.apply("path", "/c-p"), is("/c-p/path"));
        assertThat(prefix.apply("/path", "/c-p"), is("/c-p/path"));
        assertThat(prefix.apply("//path", "/c-p"), is("/c-p/path"));
        assertThat(prefix.apply("/path/", "/c-p"), is("/c-p/path/"));
        assertThat(prefix.apply("//path//", "/c-p"), is("/c-p/path/"));
    }

    @Test
    public void test_router_works() throws Exception {
        app = createAppWithContextPath("/context-path")
            .get("/hello", ctx -> ctx.result("Hello World"))
            .start();
        assertThat(GET_asString("/hello").getBody(), is("Not found. Request is below context-path (context-path: '/context-path')"));
        assertThat(GET_asString("/context-path/hello").getBody(), is("Hello World"));
        app.stop();
    }

    @Test
    public void test_twoLevelContextPath_works() throws Exception {
        app = createAppWithContextPath("/context-path/path-context")
            .get("/hello", ctx -> ctx.result("Hello World"))
            .start();
        assertThat(GET_asString("/context-path/").getStatus(), is(404));
        assertThat(GET_asString("/context-path/path-context/hello").getBody(), is("Hello World"));
        app.stop();
    }

    @Test
    public void test_staticFiles_work() throws Exception {
        app = createAppWithContextPath("/context-path").enableStaticFiles("/public").start();
        assertThat(GET_asString("/script.js").getStatus(), is(404));
        assertThat(GET_asString("/context-path/script.js").getBody(), containsString("JavaScript works"));
        app.stop();
    }

    @Test
    public void test_welcomeFile_works() throws Exception {
        app = createAppWithContextPath("/context-path").enableStaticFiles("/public").start();
        assertThat(GET_asString("/context-path/subdir/").getBody(), is("<h1>Welcome file</h1>"));
        app.stop();
    }

    private Javalin createAppWithContextPath(String contextPath) {
        return Javalin.create().port(0).contextPath(contextPath);
    }

    private static HttpResponse<String> GET_asString(String pathname) throws Exception {
        return Unirest.get("http://localhost:" + app.port() + "/" + pathname).asString();
    }

}
