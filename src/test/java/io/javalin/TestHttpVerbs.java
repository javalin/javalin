/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.HttpMethod;
import io.javalin.newutil.BaseTest;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestHttpVerbs extends BaseTest {

    @Test
    public void test_get_helloWorld() throws Exception {
        app.get("/hello", ctx -> ctx.result("Hello World"));
        assertThat(http.getBody("/hello"), is("Hello World"));
    }

    @Test
    public void test_get_helloOtherWorld() throws Exception {
        app.get("/hello", ctx -> ctx.result("Hello New World"));
        assertThat(http.getBody("/hello"), is("Hello New World"));
    }

    @Test
    public void test_all_mapped_verbs_ok() throws Exception {
        app.get("/mapped", okHandler);
        app.post("/mapped", okHandler);
        app.put("/mapped", okHandler);
        app.delete("/mapped", okHandler);
        app.patch("/mapped", okHandler);
        app.head("/mapped", okHandler);
        app.options("/mapped", okHandler);
        for (HttpMethod httpMethod : HttpMethod.values()) {
            assertThat(http.call(httpMethod, "/mapped").getStatus(), is(200));
        }
    }

    @Test
    public void test_all_unmapped_verbs_ok() throws Exception {
        for (HttpMethod httpMethod : HttpMethod.values()) {
            assertThat(http.call(httpMethod, "/unmapped").getStatus(), is(404));
        }
    }

    @Test
    public void test_headOk_ifGetMapped() throws Exception {
        app.get("/mapped", okHandler);
        assertThat(http.call(HttpMethod.HEAD, "/mapped").getStatus(), is(200));
    }

    @Test
    public void test_filterOrder_preserved() throws Exception {
        app.before(ctx -> ctx.result("1"));
        app.before(ctx -> ctx.result(ctx.resultString() + "2"));
        app.before(ctx -> ctx.result(ctx.resultString() + "3"));
        app.before(ctx -> ctx.result(ctx.resultString() + "4"));
        app.get("/hello", ctx -> ctx.result(ctx.resultString() + "Hello"));
        assertThat(http.getBody("/hello"), is("1234Hello"));
    }

}
