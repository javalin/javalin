/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core;

import io.javalin.Javalin;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.CoreRoles;
import io.javalin.core.security.Role;
import io.javalin.core.security.SecurityUtil;
import io.javalin.core.util.LogUtil;
import io.javalin.core.util.RouteOverviewConfig;
import io.javalin.core.util.RouteOverviewRenderer;
import io.javalin.http.RequestLogger;
import io.javalin.http.SinglePageHandler;
import io.javalin.http.staticfiles.JettyResourceHandler;
import io.javalin.http.staticfiles.Location;
import io.javalin.http.staticfiles.ResourceHandler;
import io.javalin.http.staticfiles.StaticFileConfig;
import io.javalin.http.util.CorsBeforeHandler;
import io.javalin.http.util.CorsOptionsHandler;
import io.javalin.plugin.metrics.JavalinMicrometer;
import io.javalin.plugin.metrics.MetricsProvider;
import io.javalin.websocket.WsHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static io.javalin.core.security.SecurityUtil.roles;

// @formatter:off
public class JavalinConfig {

    public static Consumer<JavalinConfig> noopConfig = JavalinConfig -> {}; // no change from default

    public boolean dynamicGzip = true;
    public boolean autogenerateEtags = false;
    public boolean prefer405over404 = false;
    public boolean enforceSsl = false;
    public boolean showJavalinBanner = true;
    public boolean logIfServerNotStarted = true;
    @NotNull public String defaultContentType = "text/plain";
    @NotNull public String contextPath = "/";
    @NotNull public Long requestCacheSize = 4096L;
    @NotNull public Long asyncRequestTimeout = 0L;
    @NotNull public String wsContextPath ="/";
    @NotNull public Inner inner = new Inner();
    @NotNull public MetricsProvider metricsProvider = MetricsProvider.NONE;

    // it's not bad to access this, the main reason it's hidden
    // is to provide a cleaner API with dedicated setters
    public class Inner {
        @NotNull public Map<Class<?>, Object> appAttributes = new HashMap<>();
        @NotNull public List<String> corsOrigins = new ArrayList<>();
        @Nullable public RouteOverviewConfig routeOverview = null;
        @Nullable public RequestLogger requestLogger = null;
        @Nullable public ResourceHandler resourceHandler = null;
        @NotNull public AccessManager accessManager = SecurityUtil::noopAccessManager;
        @NotNull public SinglePageHandler singlePageHandler = new SinglePageHandler();
        @Nullable public SessionHandler sessionHandler = null;
        @Nullable public Consumer<WebSocketServletFactory> wsFactoryConfig = null;
        @Nullable public WsHandler wsLogger = null;
        @Nullable public Server server = null;
    }

     public void enableDevLogging() {
        requestLogger(LogUtil::requestDevLogger);
        wsLogger(LogUtil::wsDevLogger);
    }

    public void enableWebjars() { addStaticFiles("/webjars", Location.CLASSPATH); }
    public void addStaticFiles(@NotNull String classpathPath) { addStaticFiles(classpathPath, Location.CLASSPATH); }
    public void addStaticFiles(@NotNull String path, @NotNull Location location) {
        JettyUtil.disableJettyLogger();
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
        JettyUtil.disableJettyLogger();
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

    public static void applyUserConfig(Javalin app, JavalinConfig config, Consumer<JavalinConfig> userConfig) {
        userConfig.accept(config); // apply user config to the default config
        if (config.inner.routeOverview != null) {
            app.get(config.inner.routeOverview.getPath(), new RouteOverviewRenderer(app), config.inner.routeOverview.getRoles());
        }
        if (!config.inner.corsOrigins.isEmpty()) {
            app.before(new CorsBeforeHandler(config.inner.corsOrigins));
            app.options("*", new CorsOptionsHandler(), roles(CoreRoles.NO_WRAP));
        }
        if (config.enforceSsl) {
            app.before(SecurityUtil::sslRedirect);
        }
        if (config.metricsProvider == MetricsProvider.MICROMETER) { // only have one at the moment
            config.inner.server = JettyUtil.getOrDefault(config.inner.server);
            JavalinMicrometer.init(config.inner.server);
        }
    }

}
// @formatter:on
