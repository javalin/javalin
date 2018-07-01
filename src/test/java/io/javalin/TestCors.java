/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.Unirest;
import io.javalin.newutil.TestUtil;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestCors {

    @Test(expected = IllegalArgumentException.class)
    public void test_throwsException_forEmptyOrigin() {
        Javalin.create().enableCorsForOrigin();
    }

    @Test
    public void test_enableCorsForAllOrigins() {
        new TestUtil(Javalin.create().enableCorsForAllOrigins()).test((app, http) -> {
            app.get("/", ctx -> ctx.result("Hello"));
            String path = "http://localhost:" + app.port() + "/";
            assertThat(Unirest.get(path).header("Origin", "some-origin").asString().getHeaders().get("Access-Control-Allow-Origin").get(0), is("some-origin"));
            assertThat(Unirest.get(path).header("Referer", "some-referer").asString().getHeaders().get("Access-Control-Allow-Origin").get(0), is("some-referer"));
            assertThat(Unirest.get(path).asString().getHeaders().get("Access-Control-Allow-Origin"), is(nullValue()));
        });
    }

    @Test
    public void test_enableCorsForSpecificOrigins() {
        new TestUtil(Javalin.create().enableCorsForOrigin("origin-1", "referer-1")).test((app, http) -> {
            app.get("/", ctx -> ctx.result("Hello"));
            String path = "http://localhost:" + app.port() + "/";
            assertThat(Unirest.get(path).asString().getHeaders().get("Access-Control-Allow-Origin"), is(nullValue()));
            assertThat(Unirest.get(path).header("Origin", "origin-1").asString().getHeaders().get("Access-Control-Allow-Origin").get(0), is("origin-1"));
            assertThat(Unirest.get(path).header("Referer", "referer-1").asString().getHeaders().get("Access-Control-Allow-Origin").get(0), is("referer-1"));
        });
    }

}
