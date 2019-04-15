/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core;

import io.javalin.RequestLogger;
import io.javalin.core.util.LogUtil;
import io.javalin.core.util.RouteOverviewConfig;
import io.javalin.core.util.SinglePageHandler;
import io.javalin.security.AccessManager;
import io.javalin.security.Role;
import io.javalin.security.SecurityUtil;
import io.javalin.staticfiles.JettyResourceHandler;
import io.javalin.staticfiles.Location;
import io.javalin.staticfiles.ResourceHandler;
import io.javalin.staticfiles.StaticFileConfig;
import io.javalin.websocket.WsHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jetbrains.annotations.NotNull;

// @formatter:off
public class JavalinConfig {

    public void enableDevLogging() {
        requestLogger(LogUtil::requestDevLogger);
        wsLogger(LogUtil::wsDevLogger);
    }

    // ********************************************************************************************
    // HTTP Servlet
    // ********************************************************************************************

    public boolean dynamicGzip = true;
    public boolean autogenerateEtags = false;
    public boolean prefer405over404 = false;
    public boolean enforceSsl = false;
    public String defaultContentType = "text/plain";
    public String contextPath = "/";
    public Long requestCacheSize = 4096L;
    public Long asyncRequestTimeout = 0L;

    public List<String> _corsOrigins = new ArrayList<>(); // pretend private
    public RouteOverviewConfig _routeOverviewConfig; // pretend private
    public RequestLogger _requestLogger; // pretend private
    public ResourceHandler _resourceHandler; // pretend private
    public AccessManager _accessManager = SecurityUtil::noopAccessManager; // pretend private
    public SinglePageHandler _singlePageHandler = new SinglePageHandler(); // pretend private
    public SessionHandler _sessionHandler; // pretend private

    public void enableWebjars() { addStaticFiles("/webjars", Location.CLASSPATH); }
    public void addStaticFiles(@NotNull String classpathPath) { addStaticFiles(classpathPath, Location.CLASSPATH); }
    public void addStaticFiles(@NotNull String path, @NotNull Location location) {
        if (this._resourceHandler == null) this._resourceHandler = new JettyResourceHandler();
        this._resourceHandler.addStaticFileConfig(new StaticFileConfig(path, location));
    }

    public void addSinglePageRoot(@NotNull String path, @NotNull String filePath) { addSinglePageRoot(path, filePath, Location.CLASSPATH); }
    public void addSinglePageRoot(@NotNull String path, @NotNull String filePath, @NotNull Location location) {
        this._singlePageHandler.add(path, filePath, location);
    }

    public void enableCorsForAllOrigins() { enableCorsForOrigin("*"); }
    public void enableCorsForOrigin(@NotNull String... origins) {
        if (origins.length == 0) throw new IllegalArgumentException("Origins cannot be empty.");
        this._corsOrigins = Arrays.asList(origins);
    }

    public void enableRouteOverview(@NotNull String path) { enableRouteOverview(path, new HashSet<>()); }
    public void enableRouteOverview(@NotNull String path, @NotNull Set<Role> permittedRoles) {
        this._routeOverviewConfig = new RouteOverviewConfig(path, permittedRoles);
    }

    public void accessManager(@NotNull AccessManager accessManager) {
        this._accessManager = accessManager;
    }

    public void requestLogger(@NotNull RequestLogger requestLogger) {
        this._requestLogger = requestLogger;
    }

    public void sessionHandler(@NotNull Supplier<SessionHandler> sessionHandlerSupplier) {
        this._sessionHandler = JettyUtil.getSessionHandler(sessionHandlerSupplier);
    }

    // ********************************************************************************************
    // WebSocket Servlet
    // ********************************************************************************************

    public Consumer<WebSocketServletFactory> _wsFactoryConfig; // pretend private
    public String wsContextPath ="/";
    public WsHandler wsLogger;

    public void wsFactoryConfig(@NotNull Consumer<WebSocketServletFactory> wsFactoryConfig) {
        this._wsFactoryConfig = wsFactoryConfig;
    }

    public void wsLogger(@NotNull Consumer<WsHandler> ws) {
        WsHandler logger = new WsHandler();
        ws.accept(logger);
        this.wsLogger = logger;
    }

    // ********************************************************************************************
    // Server
    // ********************************************************************************************

    Server _server; // pretend private

    public void server(Supplier<Server> server) {
        this._server = server.get();
    }

}
// @formatter:on
