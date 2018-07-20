/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.junit.Test;

import java.util.concurrent.Future;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestGracefulShutdown {
    private static final int LONG_WAIT_TIME_IN_MSECS = 500;
    private static final int CONNECT_WAIT_TIME_IN_MSEC = 30;

    @Test
    public void long_running_query_does_not_abort_during_graceful_shutdown() throws Exception {
        Javalin app = launchedJavalinApplication();
        waitForJavalinServletToLoad(app);

        Future<HttpResponse<String>> runningRequest = performLongRunningRequest(app);
        app.stop(); // request has not completed yet

        assertThat(runningRequest.get().getStatus(), equalTo(200));
    }

    private Javalin launchedJavalinApplication() {
        return Javalin.create().disableStartupBanner().port(0)
                .get("/immediate-response", context -> context.status(200))
                .get("/delayed-response", context -> Thread.sleep(LONG_WAIT_TIME_IN_MSECS))
                .start();
    }

    private void waitForJavalinServletToLoad(Javalin app) throws Exception {
        String requestUri = String.format("http://localhost:%d/%s", app.port(), "immediate-response");
        Unirest.get(requestUri).asString();
    }

    private Future<HttpResponse<String>> performLongRunningRequest(Javalin app) throws Exception {
        String requestUri = String.format("http://localhost:%d/%s", app.port(), "delayed-response");
        Future<HttpResponse<String>> responseFuture = Unirest.get(requestUri).asStringAsync();
        Thread.sleep(CONNECT_WAIT_TIME_IN_MSEC);

        return responseFuture;
    }
}
