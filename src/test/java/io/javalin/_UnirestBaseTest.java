/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.HttpMethod;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequestWithBody;
import java.io.IOException;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class _UnirestBaseTest {

    static Handler OK_HANDLER = ctx -> ctx.result("OK");

    static Javalin app;
    static String origin = null;

    static HttpClient defaultHttpClient = HttpClients.custom().build();
    static HttpClient noRedirectClient = HttpClients.custom().disableRedirectHandling().build();

    public void clearCookies() throws Exception {
        app.get("/cookie-cleaner", ctx -> ctx.cookieMap().keySet().forEach(ctx::removeCookie));
        GET_body("/cookie-cleaner");
    }

    @BeforeClass
    public static void setup() throws IOException {
        app = Javalin.create()
            .port(0)
            .enableStaticFiles("/public")
            .start();
        origin = "http://localhost:" + app.port();
    }

    @After
    public void clear() {
        ((JavalinInstance) app).clearMatcherAndMappers();
    }

    @AfterClass
    public static void tearDown() {
        app.stop();
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
