/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.hamcrest.MatcherAssert;
import org.junit.Test;

import io.javalin.embeddedserver.jetty.EmbeddedJettyFactory;

import static org.hamcrest.CoreMatchers.*;

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

}
