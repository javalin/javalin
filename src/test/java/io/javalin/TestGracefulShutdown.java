/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestGracefulShutdown {
    private static final int LONG_WAIT_TIME_IN_MSECS = 500;
    private static final int CONNECT_WAIT_TIME_IN_MSEC = 30;

    @Test
    public void long_running_query_completes_during_shutdown_when_jetty_is_auto_configured() throws Exception {
        Javalin app = sharedJavalinConfiguration().start();
        waitForJavalinServletToLoad(app);

        Future<HttpResponse<String>> runningRequest = performLongRunningRequest(app);
        app.stop(); // request has not completed yet

        assertThat(runningRequest.get().getStatus(), equalTo(200));
    }

    @Test
    public void long_running_query_completes_during_shutdown_when_jetty_is_manually_configured_with_statistics_handler() throws Exception {
        Javalin app = sharedJavalinConfiguration().server(() -> {
            Server server = new Server();
            server.insertHandler(new StatisticsHandler());
            return server;
        }).start();
        waitForJavalinServletToLoad(app);

        Future<HttpResponse<String>> runningRequest = performLongRunningRequest(app);
        app.stop(); // request has not completed yet

        assertThat(runningRequest.get().getStatus(), equalTo(200));
    }

    @Test(expected = ExecutionException.class)
    public void long_running_query_aborts_during_shutdown_when_jetty_is_manually_configured_without_statistics_handler() throws Exception {
        Javalin app = sharedJavalinConfiguration().server(Server::new).start();
        waitForJavalinServletToLoad(app);

        Future<HttpResponse<String>> runningRequest = performLongRunningRequest(app);
        app.stop(); // request has not completed yet

        runningRequest.get();
    }

    private Javalin sharedJavalinConfiguration() {
        return Javalin.create().disableStartupBanner().port(0)
                .get("/immediate-response", context -> context.status(200))
                .get("/delayed-response", context -> Thread.sleep(LONG_WAIT_TIME_IN_MSECS));
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
