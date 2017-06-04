/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import org.junit.Test;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestHaltException extends _UnirestBaseTest {

    @Test
    public void test_haltBeforeWildcard_works() throws Exception {
        app.before("/admin/*", (req, res) -> {
            throw new HaltException(401);
        });
        app.get("/admin/protected", (req, res) -> res.body("Protected resource"));
        HttpResponse<String> response = call(HttpMethod.GET, "/admin/protected");
        assertThat(response.getStatus(), is(401));
        assertThat(response.getBody(), not("Protected resource"));
    }

    @Test
    public void test_constructorsWork() throws Exception {
        HaltException haltException1 = new HaltException();
        HaltException haltException2 = new HaltException(401);
        HaltException haltException3 = new HaltException("Body");
        HaltException haltException4 = new HaltException(401, "Body");
    }

    @Test
    public void test_haltInRoute_works() throws Exception {
        app.get("/some-route", (req, res) -> {
            throw new HaltException(401, "Stop!");
        });
        HttpResponse<String> response = call(HttpMethod.GET, "/some-route");
        assertThat(response.getBody(), is("Stop!"));
        assertThat(response.getStatus(), is(401));
    }

    @Test
    public void test_afterRuns_afterHalt() throws Exception {
        app.get("/some-route", (req, res) -> {
            throw new HaltException(401, "Stop!");
        }).after((req, res) -> {
            res.status(418);
        });
        HttpResponse<String> response = call(HttpMethod.GET, "/some-route");
        assertThat(response.getBody(), is("Stop!"));
        assertThat(response.getStatus(), is(418));
    }

}
