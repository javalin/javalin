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

    @Test(expected = IOException.class)
    public void init() throws IOException {

        String endpointResponse = "Hello, slash!";
        String endpoint = "/hello";

        SimpleHttpClient simpleHttpClient = new SimpleHttpClient();
        Javalin app = Javalin.create()
                .port(7777)
                .start();

        app.setTrailingSlashesIgnored(true);

        app.get(endpoint, (ctx) -> ctx.result(endpointResponse));
        assertThat(simpleHttpClient.http_GET("localhost" + endpoint).getBody(), is(endpointResponse));
        assertThat(simpleHttpClient.http_GET("localhost" + endpoint + "/").getBody(), is(endpointResponse));
        app.setTrailingSlashesIgnored(false);
        assertThat(simpleHttpClient.http_GET("localhost" + endpoint).getBody(), is(endpointResponse));
        assertThat(simpleHttpClient.http_GET("localhost" + endpoint + "/").getBody(), not(endpointResponse));

        app.stop();
    }
}
