/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import java.net.URLEncoder;

import org.junit.Test;

import io.javalin.util.TestResponse;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class TestRouting extends _SimpleClientBaseTest {

    @Test
    public void test_aBunchOfRoutes() throws Exception {
        app.get("/", (req, res) -> res.body("/"));
        app.get("/path", (req, res) -> res.body("/path"));
        app.get("/path/:param", (req, res) -> res.body("/path/" + req.param("param")));
        app.get("/path/:param/*", (req, res) -> res.body("/path/" + req.param("param") + "/" + req.splat(0)));
        app.get("/*/*", (req, res) -> res.body("/" + req.splat(0) + "/" + req.splat(1)));
        app.get("/*/unreachable", (req, res) -> res.body("reached"));
        app.get("/*/*/:param", (req, res) -> res.body("/" + req.splat(0) + "/" + req.splat(1) + "/" + req.param("param")));
        app.get("/*/*/:param/*", (req, res) -> res.body("/" + req.splat(0) + "/" + req.splat(1) + "/" + req.param("param") + "/" + req.splat(2)));

        assertThat(simpleHttpClient.http_GET(origin + "/").getBody(), is("/"));
        assertThat(simpleHttpClient.http_GET(origin + "/path").getBody(), is("/path"));
        assertThat(simpleHttpClient.http_GET(origin + "/path/p").getBody(), is("/path/p"));
        assertThat(simpleHttpClient.http_GET(origin + "/path/p/s").getBody(), is("/path/p/s"));
        assertThat(simpleHttpClient.http_GET(origin + "/s1/s2").getBody(), is("/s1/s2"));
        assertThat(simpleHttpClient.http_GET(origin + "/s/unreachable").getBody(), not("reached"));
        assertThat(simpleHttpClient.http_GET(origin + "/s1/s2/p").getBody(), is("/s1/s2/p"));
        assertThat(simpleHttpClient.http_GET(origin + "/s1/s2/p/s3").getBody(), is("/s1/s2/p/s3"));
        assertThat(simpleHttpClient.http_GET(origin + "/s/s/s/s").getBody(), is("/s/s/s/s"));
    }


    @Test
    public void test_paramAndSplat() throws Exception {
        app.get("/:param/path/*", (req, res) -> res.body(req.param("param") + req.splat(0)));
        TestResponse response = simpleHttpClient.http_GET(origin + "/param/path/splat");
        assertThat(response.getBody(), is("paramsplat"));
    }

    @Test
    public void test_encodedParam() throws Exception {
        app.get("/:param", (req, res) -> res.body(req.param("param")));
        String paramValue = "te/st";
        TestResponse response = simpleHttpClient.http_GET(origin + "/" + URLEncoder.encode(paramValue, "UTF-8"));
        assertThat(response.getBody(), is(paramValue));
    }

    @Test
    public void test_encdedParamAndEncodedSplat() throws Exception {
        app.get("/:param/path/*", (req, res) -> res.body(req.param("param") + req.splat(0)));
        TestResponse response = simpleHttpClient.http_GET(
            origin + "/"
                + URLEncoder.encode("java/kotlin", "UTF-8")
                + "/path/"
                + URLEncoder.encode("/java/kotlin", "UTF-8")
        );
        assertThat(response.getBody(), is("java/kotlin/java/kotlin"));
    }

    @Test
    public void test_caseSensitive_paramName() throws Exception {
        app.get("/:ParaM", (req, res) -> res.body(req.param("pArAm")));
        TestResponse response = simpleHttpClient.http_GET(origin + "/param");
        assertThat(response.getBody(), is("param"));
    }

    @Test
    public void test_caseSensitive_paramValue() throws Exception {
        app.get("/:param", (req, res) -> res.body(req.param("param")));
        TestResponse response = simpleHttpClient.http_GET(origin + "/SomeCamelCasedValue");
        assertThat(response.getBody(), is("SomeCamelCasedValue"));
    }

}
