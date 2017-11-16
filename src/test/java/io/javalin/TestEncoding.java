/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.net.URLEncoder;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestEncoding extends _UnirestBaseTest {

    @Test
    public void test_param_unicode() throws Exception {
        app.get("/:param", ctx -> ctx.result(ctx.param("param")));
        assertThat(GET_body("/æøå"), is("æøå"));
        assertThat(GET_body("/♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"), is("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"));
        assertThat(GET_body("/こんにちは"), is("こんにちは"));
    }

    @Test
    public void test_queryParam_unicode() throws Exception {
        app.get("/", ctx -> ctx.result(ctx.queryParam("qp")));
        assertThat(GET_body("/?qp=æøå"), is("æøå"));
        assertThat(GET_body("/?qp=♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"), is("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"));
        assertThat(GET_body("/?qp=こんにちは"), is("こんにちは"));
    }

    @Test
    public void test_queryParam_encoded() throws Exception {
        app.get("/", ctx -> ctx.result(ctx.queryParam("qp")));
        String encoded = URLEncoder.encode("!#$&'()*+,/:;=?@[]", "UTF-8");
        assertThat(GET_body("/?qp=" + encoded), is("!#$&'()*+,/:;=?@[]"));
    }

    @Test
    public void test_queryParam_manuallyEncoded() throws Exception {
        app.get("/", ctx -> ctx.result(ctx.queryParam("qp")));
        assertThat(GET_body("/?qp=" + "8%3A00+PM"), is("8:00 PM"));
    }

    @Test
    public void test_formParam_encoded() throws Exception {
        app.post("/", ctx -> ctx.result(ctx.formParam("qp")));
        HttpResponse<String> response = Unirest
            .post(origin)
            .body("qp=8%3A00+PM")
            .asString();
        assertThat(response.getBody(), is("8:00 PM"));
    }

}
