package javalin;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.junit.Test;

import javalin.embeddedserver.jetty.EmbeddedJettyFactory;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

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
            .start()
            .awaitInitialization();
        assertThat(app.embeddedServer().attribute("is-custom-server"), is(true));
        app.stop().awaitTermination();
    }

}
