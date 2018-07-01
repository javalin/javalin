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
import io.javalin.newutil.BaseTest;
import io.javalin.newutil.TestUtil;
import java.net.URLEncoder;
import org.hamcrest.CoreMatchers;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class TestEncoding extends BaseTest {

    @Test
    public void test_param_unicode() throws Exception {
        app.get("/:path-param", ctx -> ctx.result(ctx.pathParam("path-param")));
        assertThat(http.getBody("/æøå"), is("æøå"));
        assertThat(http.getBody("/♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"), is("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"));
        assertThat(http.getBody("/こんにちは"), is("こんにちは"));
    }

    @Test
    public void test_queryParam_unicode() throws Exception {
        app.get("/", ctx -> ctx.result(ctx.queryParam("qp")));
        assertThat(http.getBody("/?qp=æøå"), is("æøå"));
        assertThat(http.getBody("/?qp=♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"), is("♚♛♜♜♝♝♞♞♟♟♟♟♟♟♟♟"));
        assertThat(http.getBody("/?qp=こんにちは"), is("こんにちは"));
    }

    @Test
    public void test_queryParam_encoded() throws Exception {
        app.get("/", ctx -> ctx.result(ctx.queryParam("qp")));
        String encoded = URLEncoder.encode("!#$&'()*+,/:;=?@[]", "UTF-8");
        assertThat(http.getBody("/?qp=" + encoded), is("!#$&'()*+,/:;=?@[]"));
    }

    @Test
    public void test_queryParam_manuallyEncoded() throws Exception {
        app.get("/", ctx -> ctx.result(ctx.queryParam("qp")));
        assertThat(http.getBody("/?qp=" + "8%3A00+PM"), is("8:00 PM"));
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

    @Test
    public void test_sane_defaults() {
        new TestUtil().test((app, http) -> {
            app.get("/text", ctx -> ctx.result("суп из капусты"));
            app.get("/json", ctx -> ctx.json("白菜湯"));
            app.get("/html", ctx -> ctx.html("kålsuppe"));
            assertThat(http.get("/text").header(Header.CONTENT_TYPE), CoreMatchers.is("text/plain"));
            assertThat(http.get("/json").header(Header.CONTENT_TYPE), CoreMatchers.is("application/json"));
            assertThat(http.get("/html").header(Header.CONTENT_TYPE), CoreMatchers.is("text/html"));
            assertThat(http.getBody("/text"), CoreMatchers.is("суп из капусты"));
            assertThat(http.getBody("/json"), CoreMatchers.is("\"白菜湯\""));
            assertThat(http.getBody("/html"), CoreMatchers.is("kålsuppe"));
        });
    }

    @Test
    public void test_sets_default() {
        new TestUtil(Javalin.create().defaultContentType("application/json")).test((app, http) -> {
            app.get("/default", ctx -> ctx.result("not json"));
            assertThat(http.get("/default").header(Header.CONTENT_TYPE), containsString("application/json"));
        });
    }

    @Test
    public void test_allows_overrides() {
        new TestUtil(Javalin.create().defaultContentType("application/json")).test((app, http) -> {
            app.get("/override", ctx -> {
                ctx.res.setCharacterEncoding("utf-8");
                ctx.res.setContentType("text/html");
            });
            assertThat(http.get("/override").header(Header.CONTENT_TYPE), containsString("utf-8"));
            assertThat(http.get("/override").header(Header.CONTENT_TYPE), containsString("text/html"));
        });
    }

}
