/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core;

import io.javalin.Javalin;
import io.javalin.core.event.HandlerMetaInfo;
import io.javalin.core.event.WsHandlerMetaInfo;
import io.javalin.core.plugin.Plugin;
import io.javalin.core.plugin.PluginAlreadyRegisteredException;
import io.javalin.core.plugin.PluginInitLifecycleViolationException;
import io.javalin.core.plugin.PluginLifecycleInit;
import io.javalin.core.plugin.PluginNotFoundException;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.SecurityUtil;
import io.javalin.core.util.CorsPlugin;
import io.javalin.core.util.LogUtil;
import io.javalin.http.RequestLogger;
import io.javalin.http.SinglePageHandler;
import io.javalin.http.staticfiles.JettyResourceHandler;
import io.javalin.http.staticfiles.Location;
import io.javalin.http.staticfiles.ResourceHandler;
import io.javalin.http.staticfiles.StaticFileConfig;
import io.javalin.plugin.metrics.JavalinMicrometer;
import io.javalin.plugin.metrics.MetricsProvider;
import io.javalin.websocket.WsHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavalinConfig {
    // @formatter:off
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
    @NotNull public String wsContextPath = "/";
    @NotNull public Inner inner = new Inner();
    @NotNull public MetricsProvider metricsProvider = MetricsProvider.NONE;

    // it's not bad to access this, the main reason it's hidden
    // is to provide a cleaner API with dedicated setters
    public class Inner {
        @NotNull public Map<Class<? extends Plugin>, Plugin> plugins = new HashMap<>();
        @NotNull public Map<Class<?>, Object> appAttributes = new HashMap<>();
        @Nullable public RequestLogger requestLogger = null;
        @Nullable public ResourceHandler resourceHandler = null;
        @NotNull public AccessManager accessManager = SecurityUtil::noopAccessManager;
        @NotNull public SinglePageHandler singlePageHandler = new SinglePageHandler();
        @Nullable public SessionHandler sessionHandler = null;
        @Nullable public Consumer<WebSocketServletFactory> wsFactoryConfig = null;
        @Nullable public WsHandler wsLogger = null;
        @Nullable public Server server = null;
    }
    // @formatter:on

    /**
     * Register a new plugin.
     */
    public void registerPlugin(@NotNull Plugin plugin) {
        if (inner.plugins.containsKey(plugin.getClass())) {
            throw new PluginAlreadyRegisteredException(plugin.getClass());
        }
        inner.plugins.put(plugin.getClass(), plugin);
    }

    /**
     * Get a registered plugin
     */
    public <T extends Plugin> T getPlugin(@NotNull Class<T> pluginClass) {
        T result = (T) inner.plugins.get(pluginClass);
        if (result == null) {
            throw new PluginNotFoundException(pluginClass);
        }
        return result;
    }

    public void enableDevLogging() {
        requestLogger(LogUtil::requestDevLogger);
        wsLogger(LogUtil::wsDevLogger);
    }

    public void enableWebjars() {
        addStaticFiles("/webjars", Location.CLASSPATH);
    }

    public void addStaticFiles(@NotNull String classpathPath) {
        addStaticFiles(classpathPath, Location.CLASSPATH);
    }

    public void addStaticFiles(@NotNull String path, @NotNull Location location) {
        JettyUtil.disableJettyLogger();
        if (inner.resourceHandler == null) inner.resourceHandler = new JettyResourceHandler();
        inner.resourceHandler.addStaticFileConfig(new StaticFileConfig(path, location));
    }

    public void addSinglePageRoot(@NotNull String path, @NotNull String filePath) {
        addSinglePageRoot(path, filePath, Location.CLASSPATH);
    }

    public void addSinglePageRoot(@NotNull String path, @NotNull String filePath, @NotNull Location location) {
        inner.singlePageHandler.add(path, filePath, location);
    }

    public void enableCorsForAllOrigins() {
        registerPlugin(CorsPlugin.forAllOrigins());
    }

    public void enableCorsForOrigin(@NotNull String... origins) {
        registerPlugin(CorsPlugin.forOrigins(origins));
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
        Collection<Plugin> plugins = config.inner.plugins.values();

        List<HandlerMetaInfo> registeredHandler = new ArrayList<>();
        List<WsHandlerMetaInfo> registeredWsHandler = new ArrayList<>();
        app.events(listener -> {
            listener.handlerAdded(registeredHandler::add);
            listener.wsHandlerAdded(registeredWsHandler::add);
        });

        plugins
            .stream()
            .filter(plugin -> plugin instanceof PluginLifecycleInit)
            .forEach(plugin -> {
                ((PluginLifecycleInit) plugin).init(app);
                if (!registeredHandler.isEmpty() || !registeredWsHandler.isEmpty()) {
                    throw new PluginInitLifecycleViolationException(plugin.getClass());
                }
            });

        plugins.forEach(plugin -> plugin.apply(app));

        if (config.enforceSsl) {
            app.before(SecurityUtil::sslRedirect);
        }
        if (config.metricsProvider == MetricsProvider.MICROMETER) { // only have one at the moment
            config.inner.server = JettyUtil.getOrDefault(config.inner.server);
            JavalinMicrometer.init(config.inner.server);
        }
    }

}
