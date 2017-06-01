/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import java.util.List;

import org.junit.Test;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.core.util.Util;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Random;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestResponse extends _UnirestBaseTest {

    private String MY_BODY = ""
        + "This is my body, and I live in it. It's 31 and 6 months old. "
        + "It's changed a lot since it was new. It's done stuff it wasn't built to do. "
        + "I often try to fill if up with wine. - Tim Minchin";

    @Test
    public void test_responseBuilder() throws Exception {
        app.get("/hello", (req, res) ->
            res.status(418)
                .body(MY_BODY)
                .header("X-HEADER-1", "my-header-1")
                .header("X-HEADER-2", "my-header-2"));
        HttpResponse<String> response = call(HttpMethod.GET, "/hello");
        assertThat(response.getStatus(), is(418));
        assertThat(response.getBody(), is(MY_BODY));
        assertThat(response.getHeaders().getFirst("X-HEADER-1"), is("my-header-1"));
        assertThat(response.getHeaders().getFirst("X-HEADER-2"), is("my-header-2"));
    }

    @Test
    public void test_responseStream() throws Exception {
        byte[] buf = new byte[65537]; // big and not on a page boundary
        new Random().nextBytes(buf);
        app.get("/stream", (req, res) -> res.body(new ByteArrayInputStream(buf)));
        HttpResponse<String> response = call(HttpMethod.GET, "/stream");
        
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        assertThat(Util.copyStream(response.getRawBody(), bout), is((long) buf.length));
        assertThat(buf, equalTo(bout.toByteArray()));
    }
    
    @Test
    public void test_redirect() throws Exception {
        app.get("/hello", (req, res) -> res.redirect("/hello-2"));
        app.get("/hello-2", (req, res) -> res.body("Redirected"));
        assertThat(GET_body("/hello"), is("Redirected"));
    }

    @Test
    public void test_redirectWithStatus() throws Exception {
        app.get("/hello", (req, res) -> res.redirect("/hello-2", 302));
        app.get("/hello-2", (req, res) -> res.body("Redirected"));
        Unirest.setHttpClient(noRedirectClient); // disable redirects
        HttpResponse<String> response = call(HttpMethod.GET, "/hello");
        assertThat(response.getStatus(), is(302));
        Unirest.setHttpClient(defaultHttpClient); // re-enable redirects
        response = call(HttpMethod.GET, "/hello");
        assertThat(response.getBody(), is("Redirected"));
    }

    @Test
    public void test_createCookie() throws Exception {
        app.post("/create-cookies", (req, res) -> res.cookie("name1", "value1").cookie("name2", "value2"));
        HttpResponse<String> response = call(HttpMethod.POST, "/create-cookies");
        List<String> cookies = response.getHeaders().get("Set-Cookie");
        assertThat(cookies, hasItem("name1=value1"));
        assertThat(cookies, hasItem("name2=value2"));
    }

    @Test
    public void test_deleteCookie() throws Exception {
        app.post("/create-cookie", (req, res) -> res.cookie("name1", "value1"));
        app.post("/delete-cookie", (req, res) -> res.removeCookie("name1"));
        HttpResponse<String> response = call(HttpMethod.POST, "/create-cookies");
        List<String> cookies = response.getHeaders().get("Set-Cookie");
        assertThat(cookies, is(nullValue()));
    }

}
