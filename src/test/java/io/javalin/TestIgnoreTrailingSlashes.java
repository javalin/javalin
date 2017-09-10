/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.javalin.util.SimpleHttpClient;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

/**
 * Created by enchanting on 03.08.17.
 */
public class TestIgnoreTrailingSlashes {

    private SimpleHttpClient simpleHttpClient = new SimpleHttpClient();

    @Test
    public void dontIgnoreTrailingSlashes() throws IOException {
        Javalin app = Javalin.create()
            .dontIgnoreTrailingSlashes()
            .start()
            .get("/hello", ctx -> ctx.result("Hello, slash!"));
        assertThat(simpleHttpClient.http_GET("http://localhost:7000/hello").getBody(), is("Hello, slash!"));
        assertThat(simpleHttpClient.http_GET("http://localhost:7000/hello/").getBody(), is("Not found"));
        app.stop();
    }

    @Test
    public void ignoreTrailingSlashes() throws IOException {
        Javalin app = Javalin.create()
            .start()
            .get("/hello", ctx -> ctx.result("Hello, slash!"));
        assertThat(simpleHttpClient.http_GET("http://localhost:7000/hello").getBody(), is("Hello, slash!"));
        assertThat(simpleHttpClient.http_GET("http://localhost:7000/hello/").getBody(), is("Hello, slash!"));
        app.stop();
    }

}
