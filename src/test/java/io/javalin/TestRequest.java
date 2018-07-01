/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.core.util.Header;
import io.javalin.util.BaseTest;
import java.util.List;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

public class TestRequest extends BaseTest {

    /*
     * Session
     */
    @Test
    public void test_session_works() throws Exception {
        app.get("/store-session", ctx -> ctx.req.getSession().setAttribute("test", "tast"));
        app.get("/read-session", ctx -> ctx.result((String) ctx.req.getSession().getAttribute("test")));
        http.getBody_withCookies("/store-session");
        assertThat(http.getBody_withCookies("/read-session"), is("tast"));
    }

    @Test
    public void test_session_isHttpOnly() throws Exception {
        app.get("/store-session", ctx -> ctx.sessionAttribute("test", "tast"));
        assertThat(http.get_withCookies("/store-session").getHeaders().getFirst("Set-Cookie").contains("HttpOnly"), is(true));
    }

    @Test
    public void test_sessionShorthand_works() throws Exception {
        app.get("/store-session", ctx -> ctx.sessionAttribute("test", "tast"));
        app.get("/read-session", ctx -> ctx.result((String) ctx.sessionAttribute("test")));
        http.getBody_withCookies("/store-session");
        assertThat(http.getBody_withCookies("/read-session"), is("tast"));
    }

    @Test
    public void test_sessionAttributeMap_works() throws Exception {
        app.get("/store-session", ctx -> {
            ctx.sessionAttribute("test", "tast");
            ctx.sessionAttribute("hest", "hast");
        });
        app.get("/read-session", ctx -> ctx.result(ctx.sessionAttributeMap().toString()));
        http.getBody_withCookies("/store-session");
        assertThat(http.getBody_withCookies("/read-session"), is("{test=tast, hest=hast}"));
    }

    @Test
    public void test_attributesCanBeNull() throws Exception {
        app.get("/store", ctx -> {
            ctx.attribute("test", "not-null");
            ctx.attribute("test", null);
            ctx.sessionAttribute("tast", "not-null");
            ctx.sessionAttribute("tast", null);
        });
        app.get("/read", ctx -> ctx.result(ctx.sessionAttribute("tast") + " and " + ctx.attribute("test")));
        http.getBody_withCookies("/store");
        assertThat(http.getBody("/read"), is("null and null"));
    }

    /*
     * Cookies
     */
    @Test
    public void test_getSingleCookie_worksForMissingCookie() throws Exception {
        app.get("/read-cookie", ctx -> ctx.result("" + ctx.cookie("my-cookie")));
        assertThat(http.getBody_withCookies("/read-cookie"), is("null"));
    }

    @Test
    public void test_getSingleCookie_worksForCookie() throws Exception {
        app.get("/read-cookie", ctx -> ctx.result(ctx.cookie("my-cookie")));
        HttpResponse<String> response = Unirest.get(origin + "/read-cookie").header(Header.COOKIE, "my-cookie=my-cookie-value").asString();
        assertThat(response.getBody(), is("my-cookie-value"));
    }

    @Test
    public void test_getMultipleCookies_worksForNoCookies() throws Exception {
        app.get("/read-cookie", ctx -> ctx.result(ctx.cookieMap().toString()));
        assertThat(http.getBody("/read-cookie"), is("{}"));
    }

    @Test
    public void test_getMultipleCookies_worksForMultipleCookies() throws Exception {
        app.get("/read-cookie", ctx -> ctx.result(ctx.cookieMap().toString()));
        HttpResponse<String> response = Unirest.get(origin + "/read-cookie").header(Header.COOKIE, "k1=v1;k2=v2;k3=v3").asString();
        assertThat(response.getBody(), containsString("k1=v1, k2=v2, k3=v3"));
    }

    /*
     * Path params
     */
    @Test
    public void test_pathParamWorks_invalidParam() throws Exception {
        app.get("/:my/:path", ctx -> ctx.result(ctx.pathParam("path-param")));
        assertThat(http.getBody("/my/path"), is("Internal server error"));
    }

    @Test
    public void test_pathParamWorks_multipleSingleParams() throws Exception {
        app.get("/:1/:2/:3", ctx -> ctx.result(ctx.pathParam("1") + ctx.pathParam("2") + ctx.pathParam("3")));
        assertThat(http.getBody("/my/path/params"), is("mypathparams"));
    }

    @Test
    public void test_pathParamMapWorks_noParamsPresent() throws Exception {
        app.get("/my/path/params", ctx -> ctx.result(ctx.pathParamMap().toString()));
        assertThat(http.getBody("/my/path/params"), is("{}"));
    }

    @Test
    public void test_pathParamMapWorks_paramsPresent() throws Exception {
        app.get("/:1/:2/:3", ctx -> ctx.result(ctx.pathParamMap().toString()));
        assertThat(http.getBody("/my/path/params"), is("{1=my, 2=path, 3=params}"));
    }

    /*
     * Query params
     */
    @Test
    public void test_queryParamWorks_noParam() throws Exception {
        app.get("/", ctx -> ctx.result("" + ctx.queryParam("qp")));
        assertThat(http.getBody("/"), is("null")); // notice {"" + req} on previous line
    }

    @Test
    public void test_queryParamWorks_noParamButDefault() throws Exception {
        app.get("/", ctx -> ctx.result("" + ctx.queryParam("qp", "default")));
        assertThat(http.getBody("/"), is("default"));
    }

    @Test
    public void test_queryParamWorks_multipleSingleParams() throws Exception {
        app.get("/", ctx -> ctx.result(ctx.queryParam("qp1") + ctx.queryParam("qp2") + ctx.queryParam("qp3")));
        assertThat(http.getBody("/?qp1=1&qp2=2&qp3=3"), is("123"));
    }

    @Test
    public void test_queryParamsWorks_noParamsPresent() throws Exception {
        app.get("/", ctx -> {
            final List<String> params = ctx.queryParams("qp1");
            ctx.result(params.toString());
        });
        assertThat(http.getBody("/"), is("[]"));
    }

    @Test
    public void test_queryParamsWorks_paramsPresent() throws Exception {
        app.get("/", ctx -> {
            final List<String> params = ctx.queryParams("qp1");
            ctx.result(params.toString());
        });
        assertThat(http.getBody("/?qp1=1&qp1=2&qp1=3"), is("[1, 2, 3]"));
    }

    @Test
    public void test_anyQueryParamNullTrue_allParamsNull() throws Exception {
        app.get("/", ctx -> ctx.result("" + ctx.anyQueryParamNull("nullkey", "othernullkey")));
        assertThat(http.getBody("/"), is("true"));
    }

    @Test
    public void test_anyQueryParamNullTrue_someParamsNull() throws Exception {
        app.get("/", ctx -> ctx.result("" + ctx.anyQueryParamNull("qp1", "qp2", "nullkey")));
        assertThat(http.getBody("/?qp1=1&qp2=2"), is("true"));
    }

    @Test
    public void test_anyQueryParamNullFalse_allParamsNonNull() throws Exception {
        app.get("/", ctx -> ctx.result("" + ctx.anyQueryParamNull("qp1", "qp2", "qp3")));
        assertThat(http.getBody("/?qp1=1&qp2=2&qp3=3"), is("false"));
    }

    /*
     * Form params
     */
    @Test
    public void test_formParamWorks() throws Exception {
        app.post("/", ctx -> ctx.result("" + ctx.formParam("fp1")));
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().getBody(), is("1"));
    }

    @Test
    public void test_formParamWorks_noParam() throws Exception {
        app.post("/", ctx -> ctx.result("" + ctx.formParam("fp3")));
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().getBody(), is("null"));
    }

    @Test
    public void test_formParamWorks_noParamButDefault() throws Exception {
        app.post("/", ctx -> ctx.result("" + ctx.formParam("fp4", "4")));
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().getBody(), is("4"));
    }

    @Test
    public void test_anyFormParamNullTrue_someParamsNull() throws Exception {
        app.post("/", ctx -> ctx.result("" + ctx.anyFormParamNull("fp1", "fp2", "nullkey")));
        assertThat(http.post("/").body("fp1=1&fp2=2").asString().getBody(), is("true"));
    }

    @Test
    public void test_anyFormParamNullFalse_allParamsNonNull() throws Exception {
        app.post("/", ctx -> ctx.result("" + ctx.anyFormParamNull("fp1", "fp2", "fp3")));
        assertThat(http.post("/").body("fp1=1&fp2=2&fp3=3").asString().getBody(), is("false"));
    }

    @Test
    public void test_basicAuth_works() throws Exception {
        app.get("/", ctx -> {
            BasicAuthCredentials basicAuthCredentials = ctx.basicAuthCredentials();
            ctx.result(basicAuthCredentials.getUsername() + "|" + basicAuthCredentials.getPassword());
        });
        HttpResponse<String> response = Unirest.get(origin + "/").basicAuth("some-username", "some-password").asString();
        assertThat(response.getBody(), is("some-username|some-password"));
    }

    @Test
    public void test_matchingPaths_works() throws Exception {
        app.get("/matched", ctx -> ctx.result(ctx.matchedPath()));
        app.get("/matched/:path-param", ctx -> ctx.result(ctx.matchedPath()));
        app.after("/matched/:path-param/:param2", ctx -> ctx.result(ctx.matchedPath()));
        assertThat(http.getBody("/matched"), is("/matched"));
        assertThat(http.getBody("/matched/p1"), is("/matched/:path-param"));
        assertThat(http.getBody("/matched/p1/p2"), is("/matched/:path-param/:param2"));
    }

    @Test
    public void test_servletContext_isNotNull() throws Exception {
        app.get("/", ctx -> ctx.result(ctx.req.getServletContext() != null ? "not-null" : "null"));
        assertThat(http.getBody("/"), is("not-null"));
    }

}
