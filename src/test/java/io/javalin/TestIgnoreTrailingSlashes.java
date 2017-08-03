package io.javalin;

import io.javalin.util.SimpleHttpClient;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

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
                .port(port)
                .dontIgnoreTrailingSlashes()
                .start();


        app.get(endpoint, (ctx) -> ctx.result(endpointResponse));
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
                .port(port)
                .start();

        app.get(endpoint, (ctx) -> ctx.result(endpointResponse));
        assertThat(simpleHttpClient.http_GET("http://localhost:" + port + endpoint).getBody(), is(endpointResponse));
        assertThat(simpleHttpClient.http_GET("http://localhost:" + port + endpoint + "/").getBody(), is(endpointResponse));

        app.stop();
    }
}
