/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import io.javalin.newutil.TestUtil;
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
    public void test_embeddedServer_withStatisticsHandler() {
        StatisticsHandler statisticsHandler = new StatisticsHandler();
        Server server = new Server();
        server.setHandler(statisticsHandler);
        new TestUtil(Javalin.create().server(() -> server)).test((app, http) -> {
            app.get("/", ctx -> ctx.result("Hello World"));
            int requests = 5;
            for (int i = 0; i < requests; i++) {
                assertThat(http.getBody("/"), is("Hello World"));
                assertThat(http.get("/not_there").code(), is(404));
            }
            assertThat(statisticsHandler.getDispatched(), is(requests * 2));
            assertThat(statisticsHandler.getResponses2xx(), is(requests));
            assertThat(statisticsHandler.getResponses4xx(), is(requests));
        });
    }

    @Test
    public void test_embeddedServer_withHandlerChain() {
        AtomicLong logCount = new AtomicLong(0);
        RequestLog requestLog = (request, response) -> logCount.incrementAndGet();
        RequestLogHandler requestLogHandler = new RequestLogHandler();
        requestLogHandler.setRequestLog(requestLog);
        StatisticsHandler handlerChain = new StatisticsHandler();
        handlerChain.setHandler(requestLogHandler);
        Server server = new Server();
        server.setHandler(handlerChain);
        new TestUtil(Javalin.create().server(() -> server)).test((app, http) -> {
            app.get("/", ctx -> ctx.result("Hello World"));
            int requests = 10;
            for (int i = 0; i < requests; i++) {
                assertThat(http.getBody("/"), is("Hello World"));
                assertThat(http.get("/not_there").code(), is(404));
            }
            assertThat(handlerChain.getDispatched(), is(requests * 2));
            assertThat(handlerChain.getResponses2xx(), is(requests));
            assertThat(handlerChain.getResponses4xx(), is(requests));
            assertThat(logCount.get(), is((long) (requests * 2)));
        });
    }
}
