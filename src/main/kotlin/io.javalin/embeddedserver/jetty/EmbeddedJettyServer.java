/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 *
 */

package io.javalin.embeddedserver.jetty;

import java.io.IOException;
import java.net.ServerSocket;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.session.SessionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javalin.Javalin;
import io.javalin.embeddedserver.EmbeddedServer;

public class EmbeddedJettyServer implements EmbeddedServer {

    private JettyServer jettyServer;
    private Server server;
    private SessionHandler javalinHandler;

    private static Logger log = LoggerFactory.getLogger(EmbeddedServer.class);

    public EmbeddedJettyServer(JettyServer jettyServer, SessionHandler javalinHandler) {
        this.jettyServer = jettyServer;
        this.javalinHandler = javalinHandler;
    }

    @Override
    public int start(String host, int port) throws Exception {

        if (port == 0) {
            try (ServerSocket serverSocket = new ServerSocket(0)) {
                port = serverSocket.getLocalPort();
            } catch (IOException e) {
                log.error("Failed to get first available port, using default port instead: " + Javalin.DEFAULT_PORT);
                port = Javalin.DEFAULT_PORT;
            }
        }

        server = jettyServer.create();

        if (server.getConnectors().length == 0) {
            ServerConnector serverConnector = EmbeddedJettyFactory.defaultConnector(server, host, port);
            server = serverConnector.getServer();
            server.setConnectors(new Connector[] {serverConnector});
        }

        server.setHandler(javalinHandler);
        server.start();

        log.info("Javalin has started \\o/");
        for (Connector connector : server.getConnectors()) {
            log.info("Localhost: " + getProtocol(connector) + "://localhost:" + ((ServerConnector) connector).getLocalPort());
        }

        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    private static String getProtocol(Connector connector) {
        return connector.getProtocols().contains("ssl") ? "https" : "http";
    }

    @Override
    public void join() throws InterruptedException {
        server.join();
    }

    @Override
    public void stop() {
        log.info("Stopping Javalin ...");
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Exception e) {
            log.error("Javalin failed to stop gracefully, calling System.exit()", e);
            System.exit(100);
        }
        log.info("Javalin stopped");
    }

    @Override
    public int activeThreadCount() {
        if (server == null) {
            return 0;
        }
        return server.getThreadPool().getThreads() - server.getThreadPool().getIdleThreads();
    }

    @Override
    public Object attribute(String key) {
        return server.getAttribute(key);
    }
}
