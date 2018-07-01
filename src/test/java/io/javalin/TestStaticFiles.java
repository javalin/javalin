/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.core.util.Header;
import io.javalin.util.TestUtil;
import io.javalin.staticfiles.Location;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class TestStaticFiles {

    @Test
    public void test_Html() {
        new TestUtil(Javalin.create().enableStaticFiles("/public")).test((app, http) -> {
            assertThat(http.get("/html.html").code(), is(200));
            assertThat(http.get("/html.html").header(Header.CONTENT_TYPE), containsString("text/html"));
            assertThat(http.getBody("/html.html"), containsString("HTML works"));
        });
    }

    @Test
    public void test_getJs() {
        new TestUtil(Javalin.create().enableStaticFiles("/public")).test((app, http) -> {
            assertThat(http.get("/script.js").code(), is(200));
            assertThat(http.get("/script.js").header(Header.CONTENT_TYPE), containsString("application/javascript"));
            assertThat(http.getBody("/script.js"), containsString("JavaScript works"));
        });
    }

    @Test
    public void test_getCss() {
        new TestUtil(Javalin.create().enableStaticFiles("/public")).test((app, http) -> {
            assertThat(http.get("/styles.css").code(), is(200));
            assertThat(http.get("/styles.css").header(Header.CONTENT_TYPE), containsString("text/css"));
            assertThat(http.getBody("/styles.css"), containsString("CSS works"));
        });
    }

    @Test
    public void test_beforeFilter() {
        new TestUtil(Javalin.create().enableStaticFiles("/public")).test((app, http) -> {
            app.before("/protected/*", ctx -> {
                throw new HaltException(401, "Protected");
            });
            assertThat(http.get("/protected/secret.html").code(), is(401));
            assertThat(http.getBody("/protected/secret.html"), is("Protected"));
        });
    }

    @Test
    public void test_rootReturns404_ifNoWelcomeFile() {
        new TestUtil(Javalin.create().enableStaticFiles("/public")).test((app, http) -> {
            assertThat(http.get("/").code(), is(404));
            assertThat(http.getBody("/"), is("Not found"));
        });
    }

    @Test
    public void test_rootReturnsWelcomeFile_ifWelcomeFileExists() throws Exception {
        new TestUtil(Javalin.create().enableStaticFiles("/public")).test((app, http) -> {
            assertThat(http.get("/subdir/").code(), is(200));
            assertThat(http.getBody("/subdir/"), is("<h1>Welcome file</h1>"));
        });
    }

    @Test
    public void test_expiresWorksAsExpected() {
        new TestUtil(Javalin.create().enableStaticFiles("/public")).test((app, http) -> {
            assertThat(http.get("/script.js").header(Header.CACHE_CONTROL), is("max-age=0"));
            assertThat(http.get("/immutable/library-1.0.0.min.js").header(Header.CACHE_CONTROL), is("max-age=31622400"));
        });
    }

    @Test
    public void test_externalFolder() {
        Javalin configuredJavalin = Javalin.create().enableStaticFiles("src/test/external/", Location.EXTERNAL);
        new TestUtil(configuredJavalin).test((app, http) -> {
            assertThat(http.get("/html.html").code(), is(200));
            assertThat(http.getBody("/html.html"), containsString("HTML works"));
        });
    }

    @Test
    public void test_multipleCallsToEnableStaticFiles() {
        Javalin configuredJavalin = Javalin.create()
            .enableStaticFiles("src/test/external/", Location.EXTERNAL)
            .enableStaticFiles("/public/immutable")
            .enableStaticFiles("/public/protected")
            .enableStaticFiles("/public/subdir");
        new TestUtil(configuredJavalin).test((app, http) -> {
            assertThat(http.get("/html.html").code(), is(200)); // src/test/external/html.html
            assertThat(http.getBody("/html.html"), containsString("HTML works"));
            assertThat(http.get("/").code(), is(200));
            assertThat(http.getBody("/"), is("<h1>Welcome file</h1>"));
            assertThat(http.get("/secret.html").code(), is(200));
            assertThat(http.getBody("/secret.html"), is("<h1>Secret file</h1>"));
            assertThat(http.get("/styles.css").code(), is(404));
        });
    }

}
