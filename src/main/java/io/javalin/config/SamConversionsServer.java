/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.config;

import java.util.function.Supplier;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.jetbrains.annotations.NotNull;

public interface SamConversionsServer {
    void server(@NotNull Supplier<Server> serverSupplier);

    void sessionHandler(@NotNull Supplier<SessionHandler> sessionHandlerSupplier);
}
