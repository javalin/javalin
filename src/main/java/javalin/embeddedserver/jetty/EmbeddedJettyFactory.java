// Javalin - https://javalin.io
// Copyright 2017 David Åse
// Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE

package javalin.embeddedserver.jetty;

import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javalin.core.ErrorMapper;
import javalin.core.ExceptionMapper;
import javalin.core.JavalinServlet;
import javalin.core.PathMatcher;
import javalin.core.util.Util;
import javalin.embeddedserver.EmbeddedServer;
import javalin.embeddedserver.EmbeddedServerFactory;

public class EmbeddedJettyFactory implements EmbeddedServerFactory {

    private JettyServer jettyServer;

    public EmbeddedJettyFactory() {
        this.jettyServer = () -> new Server(new QueuedThreadPool(200, 8, 60_000));
    }

    public EmbeddedJettyFactory(JettyServer jettyServer) {
        this.jettyServer = jettyServer;
    }

    public EmbeddedServer create(PathMatcher pathMatcher, ExceptionMapper exceptionMapper, ErrorMapper errorMapper, String staticFileDirectory) {
        JettyResourceHandler resourceHandler = new JettyResourceHandler(staticFileDirectory);
        JavalinServlet javalinServlet = new JavalinServlet(pathMatcher, exceptionMapper, errorMapper, resourceHandler);
        return new EmbeddedJettyServer(jettyServer, new JettyHandler(javalinServlet));
    }

    public static ServerConnector defaultConnector(Server server, String host, int port) {
        Util.notNull(server, "server cannot be null");
        Util.notNull(host, "host cannot be null");
        ServerConnector connector = new ServerConnector(server);
        connector.setIdleTimeout(TimeUnit.HOURS.toMillis(1));
        connector.setSoLingerTime(-1);
        connector.setHost(host);
        connector.setPort(port);
        return connector;
    }

}
