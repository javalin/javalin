/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import org.junit.Test;

import com.mashape.unirest.http.HttpMethod;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestHttpVerbs extends _UnirestBaseTest {

    @Test
    public void test_get_helloWorld() throws Exception {
        app.get("/hello", (req, res) -> res.body("Hello World"));
        assertThat(GET_body("/hello"), is("Hello World"));
    }

    @Test
    public void test_get_helloOtherWorld() throws Exception {
        app.get("/hello", (req, res) -> res.body("Hello New World"));
        assertThat(GET_body("/hello"), is("Hello New World"));
    }

    @Test
    public void test_all_mapped_verbs_ok() throws Exception {
        app.get("/mapped", OK_HANDLER);
        app.post("/mapped", OK_HANDLER);
        app.put("/mapped", OK_HANDLER);
        app.delete("/mapped", OK_HANDLER);
        app.patch("/mapped", OK_HANDLER);
        app.head("/mapped", OK_HANDLER);
        app.options("/mapped", OK_HANDLER);
        for (HttpMethod httpMethod : HttpMethod.values()) {
            assertThat(call(httpMethod, "/mapped").getStatus(), is(200));
        }
    }

    @Test
    public void test_all_unmapped_verbs_ok() throws Exception {
        for (HttpMethod httpMethod : HttpMethod.values()) {
            assertThat(call(httpMethod, "/unmapped").getStatus(), is(404));
        }
    }

    @Test
    public void test_all_nonMapped_verbs_404() throws Exception {
        for (HttpMethod httpMethod : HttpMethod.values()) {
            assertThat(call(httpMethod, "/not-mapped").getStatus(), is(404));
        }
    }

    @Test
    public void test_headOk_ifGetMapped() throws Exception {
        app.get("/mapped", OK_HANDLER);
        assertThat(call(HttpMethod.HEAD, "/mapped").getStatus(), is(200));
    }

}
