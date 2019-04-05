/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core;

import java.util.function.Supplier;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.jetbrains.annotations.NotNull;

public class JavalinServerConfig {

    public int port = 7000;
    Server server;
    SessionHandler sessionHandler;

    public void sessionHandler(@NotNull Supplier<SessionHandler> sessionHandlerSupplier) {
        this.sessionHandler = JettyUtil.getSessionHandler(sessionHandlerSupplier);
    }

    public void server(@NotNull Supplier<Server> serverSupplier) {
        this.server = serverSupplier.get();
    }

}
