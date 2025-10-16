/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.examples;

import io.javalin.Javalin;
import io.javalin.testing.TestServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import static io.javalin.testing.JavalinTestUtil.get;

public class HelloWorldServlet {

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.router.contextPath = "/api";

            Server server = new Server();
            ServletContextHandler context = new ServletContextHandler();
            context.setContextPath("/test-servlet");
            //Servlet will respond to all requests beginning with /test-servlet
            context.addServlet(TestServlet.class, "/");
            ContextHandlerCollection handlers = new ContextHandlerCollection();
            handlers.setHandlers(new Handler[]{context});
            server.setHandler(handlers);

            config.pvt.jetty.server = server;
        });
        get(app, "/", ctx -> ctx.result("Hello Javalin World!"));
        app.start(8000);
    }

}
