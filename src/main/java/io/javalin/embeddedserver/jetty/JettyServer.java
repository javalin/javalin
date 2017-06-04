/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.embeddedserver.jetty;

import org.eclipse.jetty.server.Server;

@FunctionalInterface
public interface JettyServer {
    Server create();
}
