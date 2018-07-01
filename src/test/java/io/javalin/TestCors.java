/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestCors {

    @Test
    public void test_enableCorsForAllOrigins() throws Exception {
        Javalin app1 = Javalin.create().port(0).enableCorsForAllOrigins().start();
        app1.get("/", ctx -> ctx.result("Hello"));
        HttpResponse<String> response = Unirest.get("http://localhost:" + app1.port() + "/").header("Origin", "some-origin").asString();
        assertThat(response.getHeaders().get("Access-Control-Allow-Origin").get(0), is("some-origin"));
        response = Unirest.get("http://localhost:" + app1.port() + "/").header("Referer", "some-referer").asString();
        assertThat(response.getHeaders().get("Access-Control-Allow-Origin").get(0), is("some-referer"));
        response = Unirest.get("http://localhost:" + app1.port() + "/").asString();
        assertThat(response.getHeaders().get("Access-Control-Allow-Origin"), is(nullValue()));
        app1.stop();
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_throwsException_forEmptyOrigin() {
        Javalin.create().enableCorsForOrigin();
    }

    @Test
    public void test_enableCorsForSpecificOrigins() throws Exception {
        Javalin app1 = Javalin.create().port(0).enableCorsForOrigin("origin-1", "referer-1").start();
        app1.get("/", ctx -> ctx.result("Hello"));
        HttpResponse<String> response = Unirest.get("http://localhost:" + app1.port() + "/").asString();
        assertThat(response.getHeaders().get("Access-Control-Allow-Origin"), is(nullValue()));
        response = Unirest.get("http://localhost:" + app1.port() + "/").header("Origin", "origin-1").asString();
        assertThat(response.getHeaders().get("Access-Control-Allow-Origin").get(0), is("origin-1"));
        response = Unirest.get("http://localhost:" + app1.port() + "/").header("Referer", "referer-1").asString();
        assertThat(response.getHeaders().get("Access-Control-Allow-Origin").get(0), is("referer-1"));
        app1.stop();
    }

}
