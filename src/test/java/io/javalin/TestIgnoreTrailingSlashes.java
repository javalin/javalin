/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import java.io.IOException;

import org.junit.Test;

import io.javalin.util.SimpleHttpClient;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Created by enchanting on 03.08.17.
 */
public class TestIgnoreTrailingSlashes {

    @Test()
    public void dontIgnoreTrailingSlashes() throws IOException {

        String endpointResponse = "Hello, slash!";
        String endpoint = "/hello";
        int port = 7787;
        SimpleHttpClient simpleHttpClient = new SimpleHttpClient();
        Javalin app = Javalin.create()
            .setPort(port)
            .dontIgnoreTrailingSlashes()
            .start();

        app.get(endpoint, ctx -> ctx.result(endpointResponse));
        assertThat(simpleHttpClient.http_GET("http://localhost:" + port + endpoint).getBody(), is(endpointResponse));
        assertThat(simpleHttpClient.http_GET("http://localhost:" + port + endpoint + "/").getBody(), not(endpointResponse));

        app.stop();
    }

    @Test()
    public void ignoreTrailingSlashes() throws IOException {

        String endpointResponse = "Hello, slash!";
        String endpoint = "/hello";
        int port = 7787;
        SimpleHttpClient simpleHttpClient = new SimpleHttpClient();
        Javalin app = Javalin.create()
            .setPort(port)
            .start();

        app.get(endpoint, ctx -> ctx.result(endpointResponse));
        assertThat(simpleHttpClient.http_GET("http://localhost:" + port + endpoint).getBody(), is(endpointResponse));
        assertThat(simpleHttpClient.http_GET("http://localhost:" + port + endpoint + "/").getBody(), is(endpointResponse));

        app.stop();
    }
}
