/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin;

import io.javalin.core.JettyUtil;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.Role;
import io.javalin.core.security.SecurityUtil;
import io.javalin.core.util.LogUtil;
import io.javalin.core.util.RouteOverviewConfig;
import io.javalin.http.RequestLogger;
import io.javalin.http.SinglePageHandler;
import io.javalin.http.staticfiles.JettyResourceHandler;
import io.javalin.http.staticfiles.Location;
import io.javalin.http.staticfiles.ResourceHandler;
import io.javalin.http.staticfiles.StaticFileConfig;
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
import org.jetbrains.annotations.Nullable;

// @formatter:off
public class JavalinConfig {

    public boolean dynamicGzip = true;
    public boolean autogenerateEtags = false;
    public boolean prefer405over404 = false;
    public boolean enforceSsl = false;
    public boolean showJavalinBanner = true;
    @NotNull public String defaultContentType = "text/plain";
    @NotNull public String contextPath = "/";
    @NotNull public Long requestCacheSize = 4096L;
    @NotNull public Long asyncRequestTimeout = 0L;
    @NotNull public String wsContextPath ="/";
    @NotNull public Inner inner = new Inner();

    // it's not bad to access this, the main reason it's hidden
    // is to provide a cleaner API with dedicated setters
    public class Inner {
        @NotNull public List<String> corsOrigins = new ArrayList<>();
        @Nullable public RouteOverviewConfig routeOverview;
        @Nullable public RequestLogger requestLogger;
        @Nullable public ResourceHandler resourceHandler;
        @NotNull public AccessManager accessManager = SecurityUtil::noopAccessManager;
        @NotNull public SinglePageHandler singlePageHandler = new SinglePageHandler();
        @Nullable public SessionHandler sessionHandler;
        @Nullable public Consumer<WebSocketServletFactory> wsFactoryConfig;
        @Nullable public WsHandler wsLogger;
        @Nullable public Server server;
    }

     public void enableDevLogging() {
        requestLogger(LogUtil::requestDevLogger);
        wsLogger(LogUtil::wsDevLogger);
    }

    public void enableWebjars() { addStaticFiles("/webjars", Location.CLASSPATH); }
    public void addStaticFiles(@NotNull String classpathPath) { addStaticFiles(classpathPath, Location.CLASSPATH); }
    public void addStaticFiles(@NotNull String path, @NotNull Location location) {
        if (inner.resourceHandler == null) inner.resourceHandler = new JettyResourceHandler();
        inner.resourceHandler.addStaticFileConfig(new StaticFileConfig(path, location));
    }

    public void addSinglePageRoot(@NotNull String path, @NotNull String filePath) { addSinglePageRoot(path, filePath, Location.CLASSPATH); }
    public void addSinglePageRoot(@NotNull String path, @NotNull String filePath, @NotNull Location location) {
        inner.singlePageHandler.add(path, filePath, location);
    }

    public void enableCorsForAllOrigins() { enableCorsForOrigin("*"); }
    public void enableCorsForOrigin(@NotNull String... origins) {
        if (origins.length == 0) throw new IllegalArgumentException("Origins cannot be empty.");
        inner.corsOrigins = Arrays.asList(origins);
    }

    public void enableRouteOverview(@NotNull String path) { enableRouteOverview(path, new HashSet<>()); }
    public void enableRouteOverview(@NotNull String path, @NotNull Set<Role> permittedRoles) {
        inner.routeOverview = new RouteOverviewConfig(path, permittedRoles);
    }

    public void accessManager(@NotNull AccessManager accessManager) {
        inner.accessManager = accessManager;
    }

    public void requestLogger(@NotNull RequestLogger requestLogger) {
        inner.requestLogger = requestLogger;
    }

    public void sessionHandler(@NotNull Supplier<SessionHandler> sessionHandlerSupplier) {
        inner.sessionHandler = JettyUtil.getSessionHandler(sessionHandlerSupplier);
    }

    public void wsFactoryConfig(@NotNull Consumer<WebSocketServletFactory> wsFactoryConfig) {
        inner.wsFactoryConfig = wsFactoryConfig;
    }

    public void wsLogger(@NotNull Consumer<WsHandler> ws) {
        WsHandler logger = new WsHandler();
        ws.accept(logger);
        inner.wsLogger = logger;
    }

    public void server(Supplier<Server> server) {
        inner.server = server.get();
    }

}
// @formatter:on
