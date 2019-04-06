/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core;

import io.javalin.RequestLogger;
import io.javalin.core.util.SinglePageHandler;
import io.javalin.security.AccessManager;
import io.javalin.security.SecurityUtil;
import io.javalin.staticfiles.JettyResourceHandler;
import io.javalin.staticfiles.Location;
import io.javalin.staticfiles.ResourceHandler;
import io.javalin.staticfiles.StaticFileConfig;
import io.javalin.websocket.WsHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jetbrains.annotations.NotNull;

// @formatter:off
public class JavalinConfig {

    // ********************************************************************************************
    // HTTP Servlet
    // ********************************************************************************************

    public boolean dynamicGzip = true;
    public boolean autogenerateEtags = false;
    public boolean prefer405over404 = false;
    public String defaultContentType = "text/plain";
    public String contextPath = "/";
    public Long requestCacheSize = 4096L;
    public List<String> corsOrigins = new ArrayList<>();
    RequestLogger requestLogger;
    ResourceHandler resourceHandler;
    AccessManager accessManager = SecurityUtil::noopAccessManager;
    SinglePageHandler singlePageHandler = new SinglePageHandler();
    SessionHandler sessionHandler;

    public void enableWebjars() { addStaticFiles("/webjars", Location.CLASSPATH); }
    public void addStaticFiles(@NotNull String classpathPath) { addStaticFiles(classpathPath, Location.CLASSPATH); }
    public void addStaticFiles(@NotNull String path, @NotNull Location location) {
        if (resourceHandler == null) this.resourceHandler = new JettyResourceHandler();
        this.resourceHandler.addStaticFileConfig(new StaticFileConfig(path, location));
    }

    public void addSinglePageRoot(@NotNull String path, @NotNull String filePath) { addSinglePageRoot(path, filePath, Location.CLASSPATH); }
    public void addSinglePageRoot(@NotNull String path, @NotNull String filePath, @NotNull Location location) {
        this.singlePageHandler.add(path, filePath, location);
    }

    public void enableCorsForAllOrigins() { enableCorsForOrigin("*"); }
    public void enableCorsForOrigin(@NotNull String... origins) {
        if (origins.length == 0) throw new IllegalArgumentException("Origins cannot be empty.");
        this.corsOrigins = Arrays.asList(origins);
    }

    public void accessManager(@NotNull AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    public void requestLogger(@NotNull RequestLogger requestLogger) {
        this.requestLogger = requestLogger;
    }

    public void sessionHandler(@NotNull Supplier<SessionHandler> sessionHandlerSupplier) {
        this.sessionHandler = JettyUtil.getSessionHandler(sessionHandlerSupplier);
    }

    // ********************************************************************************************
    // WebSocket Servlet
    // ********************************************************************************************

    Consumer<WebSocketServletFactory> wsFactoryConfig;
    public String wsContextPath ="/";
    public WsHandler wsLogger;

    public void wsFactoryConfig(@NotNull Consumer<WebSocketServletFactory> wsFactoryConfig) {
        this.wsFactoryConfig = wsFactoryConfig;
    }

    public void wsLogger(@NotNull Consumer<WsHandler> ws) {
        WsHandler logger = new WsHandler();
        ws.accept(logger);
        this.wsLogger = logger;
    }

}
// @formatter:on
