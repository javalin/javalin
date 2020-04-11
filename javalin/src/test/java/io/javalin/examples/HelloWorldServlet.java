/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.misc.TestServlet;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;

public class HelloWorldServlet {

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.server(() -> {
                Server server = new Server();
                ServletContextHandler context = new ServletContextHandler();
                context.setContextPath("/other-servlet");
                context.addServlet(TestServlet.class, "/");
                ContextHandlerCollection handlers = new ContextHandlerCollection();
                handlers.setHandlers(new Handler[]{context});
                server.setHandler(handlers);
                return server;
            });
        });
        app.get("/", ctx -> ctx.result("Hello Javalin World!"));
        app.start(8000);
    }

}
