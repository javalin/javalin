/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;

public class _UnirestBaseTest {

    static Handler OK_HANDLER = ctx -> ctx.result("OK");

    static Javalin app;
    static String origin = "http://localhost:7777";

    static HttpClient defaultHttpClient = HttpClients.custom().build();
    static HttpClient noRedirectClient = HttpClients.custom().disableRedirectHandling().build();

    @BeforeClass
    public static void setup() throws IOException {
        app = Javalin.create()
            .port(7777)
            .start()
            .awaitInitialization();
    }

    @After
    public void clearRoutes() {
        app.pathMatcher.clear();
        app.errorMapper.clear();
        app.exceptionMapper.clear();
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
        app.awaitTermination();
    }

    static String GET_body(String pathname) throws UnirestException {
        return Unirest.get(origin + pathname).asString().getBody();
    }

    static HttpResponse<String> GET_asString(String pathname) throws UnirestException {
        return Unirest.get(origin + pathname).asString();
    }

    static HttpResponse<String> call(HttpMethod method, String pathname) throws UnirestException {
        return new HttpRequestWithBody(method, origin + pathname).asString();
    }

}
