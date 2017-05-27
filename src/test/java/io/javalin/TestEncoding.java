/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import java.net.URLEncoder;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestEncoding extends _UnirestBaseTest {

    @Test
    public void test_param_unicode() throws Exception {
        app.get("/:param", (req, res) -> res.body(req.param("param")));
        assertThat(GET_body("/æøå"), is("æøå"));
        assertThat(GET_body("/♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"), is("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"));
        assertThat(GET_body("/こんにちは"), is("こんにちは"));
    }

    @Test
    public void test_queryParam_unicode() throws Exception {
        app.get("/", (req, res) -> res.body(req.queryParam("qp")));
        assertThat(GET_body("/?qp=æøå"), is("æøå"));
        assertThat(GET_body("/?qp=♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"), is("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"));
        assertThat(GET_body("/?qp=こんにちは"), is("こんにちは"));
    }

    @Test
    public void test_queryParam_encoded() throws Exception {
        app.get("/", (req, res) -> res.body(req.queryParam("qp")));
        String encoded = URLEncoder.encode("!#$&'()*+,/:;=?@[]", "UTF-8");
        assertThat(GET_body("/?qp=" + encoded), is("!#$&'()*+,/:;=?@[]"));
    }

}