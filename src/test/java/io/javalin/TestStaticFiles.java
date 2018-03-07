/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.core.util.Header;
import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;


public class TestStaticFiles {

    private static Javalin app;
    private static String origin = null;

    @BeforeClass
    public static void setup() throws IOException {
        app = Javalin.create()
            .port(0)
            .enableStaticFiles("/public")
            .start();
        origin = "http://localhost:" + app.port();
    }

    @After
    public void clear() {
        app.clearMatcherAndMappers();
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
    }

    @Test
    public void test_Html() throws Exception {
        HttpResponse<String> response = Unirest.get(origin + "/html.html").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getHeaders().getFirst("Content-Type"), containsString("text/html"));
        assertThat(response.getBody(), containsString("HTML works"));

    }

    @Test
    public void test_getJs() throws Exception {
        HttpResponse<String> response = Unirest.get(origin + "/script.js").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getHeaders().getFirst("Content-Type"), containsString("application/javascript"));
        assertThat(response.getBody(), containsString("JavaScript works"));
    }

    @Test
    public void test_getCss() throws Exception {
        HttpResponse<String> response = Unirest.get(origin + "/styles.css").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getHeaders().getFirst("Content-Type"), containsString("text/css"));
        assertThat(response.getBody(), containsString("CSS works"));
    }

    @Test
    public void test_beforeFilter() throws Exception {
        app.before("/protected/*", ctx -> {
            throw new HaltException(401, "Protected");
        });
        HttpResponse<String> response = Unirest.get(origin + "/protected/secret.html").asString();
        assertThat(response.getStatus(), is(401));
        assertThat(response.getBody(), is("Protected"));
    }

    @Test
    public void test_rootReturns404_ifNoWelcomeFile() throws Exception {
        HttpResponse<String> response = Unirest.get(origin + "/").asString();
        assertThat(response.getStatus(), is(404));
        assertThat(response.getBody(), is("Not found"));
    }

    @Test
    public void test_rootReturnsWelcomeFile_ifWelcomeFileExists() throws Exception {
        HttpResponse<String> response = Unirest.get(origin + "/subdir/").asString();
        assertThat(response.getStatus(), is(200));
        assertThat(response.getBody(), is("<h1>Welcome file</h1>"));
    }

    @Test
    public void test_expiresWorksAsExpected() throws Exception {
        HttpResponse<String> response1 = Unirest.get(origin + "/script.js").asString();
        assertThat(response1.getHeaders().get(Header.CACHE_CONTROL).get(0), is("max-age=0"));
        HttpResponse<String> response2 = Unirest.get(origin + "/immutable/library-1.0.0.min.js").asString();
        assertThat(response2.getHeaders().get(Header.CACHE_CONTROL).get(0), is("max-age=31622400"));
    }

}
