/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import io.javalin.newutil.BaseTest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsCollectionContaining.hasItems;

public class TestResponse extends BaseTest {

    private String MY_BODY = ""
        + "This is my body, and I live in it. It's 31 and 6 months old. "
        + "It's changed a lot since it was new. It's done stuff it wasn't built to do. "
        + "I often try to fill if up with wine. - Tim Minchin";

    @Test
    public void test_resultString() throws Exception {
        app.get("/hello", ctx ->
            ctx.status(418)
                .result(MY_BODY)
                .header("X-HEADER-1", "my-header-1")
                .header("X-HEADER-2", "my-header-2"));
        HttpResponse<String> response = http.call(HttpMethod.GET, "/hello");
        assertThat(response.getStatus(), is(418));
        assertThat(response.getBody(), is(MY_BODY));
        assertThat(response.getHeaders().getFirst("X-HEADER-1"), is("my-header-1"));
        assertThat(response.getHeaders().getFirst("X-HEADER-2"), is("my-header-2"));
    }

    @Test
    public void test_resultStream() throws Exception {
        byte[] buf = new byte[65537]; // big and not on a page boundary
        new Random().nextBytes(buf);
        app.get("/stream", ctx -> ctx.result(new ByteArrayInputStream(buf)));
        HttpResponse<String> response = http.call(HttpMethod.GET, "/stream");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        assertThat(IOUtils.copy(response.getRawBody(), bout), is(buf.length));
        assertThat(buf, equalTo(bout.toByteArray()));
    }

    @Test
    public void test_redirectInBefore() throws Exception {
        app.before("/before", ctx -> ctx.redirect("/redirected"));
        app.get("/redirected", ctx -> ctx.result("Redirected"));
        assertThat(http.getBody("/before"), is("Redirected"));
    }

    @Test
    public void test_redirectInExceptionMapper() throws Exception {
        app.get("/get", ctx -> {
            throw new Exception();
        });
        app.exception(Exception.class, (exception, ctx) -> ctx.redirect("/redirected"));
        app.get("/redirected", ctx -> ctx.result("Redirected"));
        assertThat(http.getBody("/get"), is("Redirected"));
    }

    @Test
    public void test_redirect() throws Exception {
        app.get("/hello", ctx -> ctx.redirect("/hello-2"));
        app.get("/hello-2", ctx -> ctx.result("Redirected"));
        assertThat(http.getBody("/hello"), is("Redirected"));
    }

    @Test
    public void test_redirectWithStatus() throws Exception {
        app.get("/hello", ctx -> ctx.redirect("/hello-2", 301));
        app.get("/hello-2", ctx -> ctx.result("Redirected"));
        http.disableUnirestRedirects();
        assertThat(http.call(HttpMethod.GET, "/hello").getStatus(), is(301));
        http.enableUnirestRedirects();
        assertThat(http.call(HttpMethod.GET, "/hello").getBody(), is("Redirected"));
    }

    @Test
    public void test_redirectWithStatus_absolutePath() throws Exception {
        app.get("/hello-abs", ctx -> ctx.redirect(origin + "/hello-abs-2", 303));
        app.get("/hello-abs-2", ctx -> ctx.result("Redirected"));
        http.disableUnirestRedirects();
        assertThat(http.call(HttpMethod.GET, "/hello-abs").getStatus(), is(303));
        http.enableUnirestRedirects();
        assertThat(http.call(HttpMethod.GET, "/hello-abs").getBody(), is("Redirected"));
    }

    @Test
    public void test_createCookie() throws Exception {
        app.post("/create-cookies", ctx -> ctx.cookie("name1", "value1").cookie("name2", "value2"));
        assertThat(http.post("/create-cookies").asString().getHeaders().get("Set-Cookie"), hasItems("name1=value1", "name2=value2"));
    }

    @Test
    public void test_cookie() throws Exception {
        app.post("/create-cookie", ctx -> ctx.cookie("Test", "Tast"));
        assertThat(http.post("/create-cookie").asString().getHeaders().get("Set-Cookie"), hasItem("Test=Tast"));
    }

}
