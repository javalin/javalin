/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import com.mashape.unirest.http.Unirest;
import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestCustomJetty {

    @Test
    public void test_embeddedServer_withCustomServer() throws Exception {
        Javalin app = Javalin.create()
            .port(0)
            .embeddedServer(new EmbeddedJettyFactory(() -> {
                Server server = new Server();
                server.setAttribute("is-custom-server", true);
                return server;
            }))
            .start();
        assertThat(app.embeddedServer().attribute("is-custom-server"), is(true));
        app.stop();
    }

    @Test
    public void test_embeddedServer_withStatisticsHandler() throws Exception {
        StatisticsHandler statisticsHandler = new StatisticsHandler();

        Javalin app = Javalin.create()
            .port(0)
            .embeddedServer(new EmbeddedJettyFactory(() -> {
                Server server = new Server();
                server.setHandler(statisticsHandler);
                return server;
            }))
            .get("/", ctx -> ctx.result("Hello World"))
            .start();

        String origin = "http://localhost:" + app.port();

        int requests = 5;
        for (int i = 0; i < requests; i++) {
            assertThat(Unirest.get(origin + "/").asString().getBody(), is("Hello World"));
            assertThat(Unirest.get(origin + "/not_there").asString().getStatus(), is(404));
        }

        assertThat(statisticsHandler.getDispatched(), is(requests * 2));
        assertThat(statisticsHandler.getResponses2xx(), is(requests));
        assertThat(statisticsHandler.getResponses4xx(), is(requests));
        app.stop();
    }

    @Test
    public void test_embeddedServer_withHandlerChain() throws Exception {
        AtomicLong logCount = new AtomicLong(0);
        RequestLog requestLog = (request, response) -> logCount.incrementAndGet();
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(requestLog);
        StatisticsHandler handlerChain = new StatisticsHandler();
        handlerChain.setHandler(requestLogHandler);

        Javalin app = Javalin.create()
            .port(0)
            .embeddedServer(new EmbeddedJettyFactory(() -> {
                Server server = new Server();
                server.setHandler(handlerChain);
                return server;
            }))
            .get("/", ctx -> ctx.result("Hello World"))
            .start();

        String origin = "http://localhost:" + app.port();

        int requests = 10;
        for (int i = 0; i < requests; i++) {
            assertThat(Unirest.get(origin + "/").asString().getBody(), is("Hello World"));
            assertThat(Unirest.get(origin + "/not_there").asString().getStatus(), is(404));
        }

        assertThat(handlerChain.getDispatched(), is(requests * 2));
        assertThat(handlerChain.getResponses2xx(), is(requests));
        assertThat(handlerChain.getResponses4xx(), is(requests));

        assertThat(logCount.get(), is((long) (requests * 2)));

        app.stop();
    }
}
