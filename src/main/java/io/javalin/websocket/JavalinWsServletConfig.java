/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.websocket;

import java.util.function.Consumer;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jetbrains.annotations.NotNull;

public class JavalinWsServletConfig {

    public String contextPath = "/";
    Consumer<WebSocketServletFactory> wsFactoryConfig;
    private JavalinWsServlet wsServlet;
    public JavalinWsServletConfig(JavalinWsServlet wsServlet) {
        this.wsServlet = wsServlet;
    }

    public void wsFactoryConfig(@NotNull Consumer<WebSocketServletFactory> wsFactoryConfig) {
        this.wsFactoryConfig = wsFactoryConfig;
    }

    public void wsLogger(@NotNull Consumer<WsHandler> ws) {
        WsHandler logger = new WsHandler();
        ws.accept(logger);
        wsServlet.getWsPathMatcher().setWsLogger(logger);
    }

}
