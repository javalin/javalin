/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class TestGracefulShutdown {
    private static final int LONG_WAIT_TIME_IN_MSECS = 500;
    private static final int CONNECT_WAIT_TIME_IN_MSEC = 30;

    private Javalin app;

    @Before
    public void configureJavalinBeforeEachTest() {
        app = Javalin.create().disableStartupBanner().port(0);
        app.get("/immediate-response", context -> context.status(200));
        app.get("/delayed-response", context -> Thread.sleep(LONG_WAIT_TIME_IN_MSECS));
        app.start();
    }

    @Test
    public void long_running_query_does_not_abort_during_graceful_shutdown() throws Exception {
        waitForServletToLoad();
        validateLongRunningRequestCompletes();
        waitForSocketToConnect();
        performShutdown();
    }

    private void waitForServletToLoad() throws Exception {
        String baseUri = String.format("http://localhost:%d", app.port());
        Unirest.get(baseUri + "/immediate-response").asString();
    }

    private void validateLongRunningRequestCompletes() {
        String baseUri = String.format("http://localhost:%d", app.port());
        Unirest.get(baseUri + "/delayed-response").asStringAsync(new AsyncResponseHandler());
    }

    private void waitForSocketToConnect() throws Exception {
        Thread.sleep(CONNECT_WAIT_TIME_IN_MSEC);
    }

    private void performShutdown() {
        app.stop();
    }

    static class AsyncResponseHandler implements Callback<String> {
        public void completed(HttpResponse<String> response) {
            assertThat(response.getStatus(), equalTo(200));
        }

        public void failed(UnirestException exception) {
            fail("request failed: " + exception.getMessage());
        }

        public void cancelled() {
            fail("request was cancelled");
        }
    }
}
