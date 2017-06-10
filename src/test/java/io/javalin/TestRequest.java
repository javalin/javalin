/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import java.util.Arrays;

import org.junit.Test;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestRequest extends _UnirestBaseTest {

    /*
     * Cookies
     */
    @Test
    public void test_getSingleCookie_worksForMissingCookie() throws Exception {
        app.get("/read-cookie", ctx -> ctx.result("" + ctx.cookie("my-cookie")));
        assertThat(GET_body("/read-cookie"), is("null")); // notice {"" + req} on previous line
    }

    @Test
    public void test_getSingleCookie_worksForCookie() throws Exception {
        app.get("/read-cookie", ctx -> ctx.result(ctx.cookie("my-cookie")));
        HttpResponse<String> response = Unirest.get(origin + "/read-cookie").header("Cookie", "my-cookie=my-cookie-value").asString();
        assertThat(response.getBody(), is("my-cookie-value"));
    }

    @Test
    public void test_getMultipleCookies_worksForNoCookies() throws Exception {
        app.get("/read-cookie", ctx -> ctx.result(ctx.cookieMap().toString()));
        assertThat(GET_body("/read-cookie"), is("{}"));
    }

    @Test
    public void test_getMultipleCookies_worksForMultipleCookies() throws Exception {
        app.get("/read-cookie", ctx -> ctx.result(ctx.cookieMap().toString()));
        HttpResponse<String> response = Unirest.get(origin + "/read-cookie").header("Cookie", "k1=v1;k2=v2;k3=v3").asString();
        assertThat(response.getBody(), is("{k1=v1, k2=v2, k3=v3}"));
    }

    /*
    * Path params
    */
    @Test
    public void test_paramWorks_noParam() throws Exception {
        app.get("/my/path", ctx -> ctx.result("" + ctx.param("param")));
        assertThat(GET_body("/my/path"), is("null")); // notice {"" + req} on previous line
    }

    @Test
    public void test_paramWorks_nullKey() throws Exception {
        app.get("/my/path", ctx -> ctx.result("" + ctx.param(null)));
        assertThat(GET_body("/my/path"), is("Internal server error")); // notice {"" + req} on previous line
    }

    @Test
    public void test_paramWorks_multipleSingleParams() throws Exception {
        app.get("/:1/:2/:3", ctx -> ctx.result(ctx.param("1") + ctx.param("2") + ctx.param("3")));
        assertThat(GET_body("/my/path/params"), is("mypathparams"));
    }

    @Test
    public void test_paramMapWorks_noParamsPresent() throws Exception {
        app.get("/my/path/params", ctx -> ctx.result(ctx.paramMap().toString()));
        assertThat(GET_body("/my/path/params"), is("{}"));
    }

    @Test
    public void test_paramMapWorks_paramsPresent() throws Exception {
        app.get("/:1/:2/:3", ctx -> ctx.result(ctx.paramMap().toString()));
        assertThat(GET_body("/my/path/params"), is("{:1=my, :2=path, :3=params}"));
    }

    /*
    * Query params
    */
    @Test
    public void test_queryParamWorks_noParam() throws Exception {
        app.get("/", ctx -> ctx.result("" + ctx.queryParam("qp")));
        assertThat(GET_body("/"), is("null")); // notice {"" + req} on previous line
    }

    @Test
    public void test_queryParamWorks_multipleSingleParams() throws Exception {
        app.get("/", ctx -> ctx.result(ctx.queryParam("qp1") + ctx.queryParam("qp2") + ctx.queryParam("qp3")));
        assertThat(GET_body("/?qp1=1&qp2=2&qp3=3"), is("123"));
    }

    @Test
    public void test_queryParamsWorks_noParamsPresent() throws Exception {
        app.get("/", ctx -> ctx.result(Arrays.toString(ctx.queryParams("qp1"))));
        assertThat(GET_body("/"), is("null"));
    }

    @Test
    public void test_queryParamsWorks_paramsPresent() throws Exception {
        app.get("/", ctx -> ctx.result(Arrays.toString(ctx.queryParams("qp1"))));
        assertThat(GET_body("/?qp1=1&qp1=2&qp1=3"), is("[1, 2, 3]"));
    }

    @Test
    public void test_nextWorks_whenMultipleHandlers() throws Exception {
        app.get("/test", ctx -> ctx.next());
        app.get("/*", ctx -> ctx.result("Skipped first handler"));
        assertThat(GET_body("/test"), is("Skipped first handler"));
    }

    @Test
    public void test_nextGivesBlankResponse_whenNoHandlers() throws Exception {
        app.get("/test", ctx -> ctx.next());
        assertThat(GET_body("/test"), is(""));
    }

}
