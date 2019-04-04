/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core;

import java.util.function.Supplier;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;

public class JavalinServerConfig {

    Server server;
    SessionHandler sessionHandler;
    public int port = 7000;
    public String contextPath = "/";

    public void sessionHandler(Supplier<SessionHandler> sessionHandlerSupplier) {
        this.sessionHandler = sessionHandlerSupplier.get();
    }

    public void server(Supplier<Server> serverSupplier) {
        this.server = serverSupplier.get();
    }

}
