/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import static io.javalin.ApiBuilder.get;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class TestCustomJetty {

    @Test
    public void test_embeddedServer_setsCustomServer() throws Exception {
        Javalin app = Javalin.create()
            .port(0)
            .embeddedServer(new EmbeddedJettyFactory(() -> {
                Server server = new Server(new QueuedThreadPool(200, 8, 60_000));
                server.setAttribute("is-custom-server", true);
                return server;
            }))
            .start();
        MatcherAssert.assertThat(app.embeddedServer().attribute("is-custom-server"), is(true));
        app.stop();
    }

    @Test
    public void test_embeddedServer_setsStatisticsHandler() throws Exception {
        StatisticsHandler handler = new StatisticsHandler();
        Javalin app = Javalin.create()
            .port(0)
            .embeddedServer(new EmbeddedJettyFactory(handler))
            .routes(() -> get("/", ctx -> ctx.result("hello world")))
            .start();

        final String origin = "http://localhost:" + app.port();

        // make sure the internal handlers dispatch...
        String s = Unirest.get(origin + "/").asString().getBody();
        MatcherAssert.assertThat(s, is(equalTo("hello world")));

        HttpResponse<String> response = Unirest.get(origin + "/not_there").asString();
        MatcherAssert.assertThat(response.getStatus(), is(equalTo(404)));

        MatcherAssert.assertThat(handler.getDispatched(), is(equalTo(2)));
        MatcherAssert.assertThat(handler.getResponses2xx(), is(equalTo(1)));
        MatcherAssert.assertThat(handler.getResponses4xx(), is(equalTo(1)));
        
        app.stop();
    }

    @Test
    public void test_embeddedServer_setsStatisticsHandlerAndCustomServer() throws Exception {
        StatisticsHandler handler = new StatisticsHandler();
        Javalin app = Javalin.create()
            .port(0)
            .embeddedServer(new EmbeddedJettyFactory(handler, () -> {
                Server server = new Server(new QueuedThreadPool(200, 8, 60_000));
                server.setAttribute("is-custom-server", true);
                return server;
            }))
            .routes(() -> get("/", ctx -> ctx.result("hello world")))
            .start();

        final String origin = "http://localhost:" + app.port();

        // make sure the internal handlers dispatch...
        String s = Unirest.get(origin + "/").asString().getBody();
        MatcherAssert.assertThat(s, is(equalTo("hello world")));

        HttpResponse<String> response = Unirest.get(origin + "/not_there").asString();
        MatcherAssert.assertThat(response.getStatus(), is(equalTo(404)));

        MatcherAssert.assertThat(app.embeddedServer().attribute("is-custom-server"), is(true));

        MatcherAssert.assertThat(handler.getDispatched(), is(equalTo(2)));
        MatcherAssert.assertThat(handler.getResponses2xx(), is(equalTo(1)));
        MatcherAssert.assertThat(handler.getResponses4xx(), is(equalTo(1)));

        app.stop();
    }

}
