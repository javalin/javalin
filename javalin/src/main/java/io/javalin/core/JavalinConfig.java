/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core;

import io.javalin.Javalin;
import io.javalin.core.compression.Brotli;
import io.javalin.core.compression.CompressionStrategy;
import io.javalin.core.compression.Gzip;
import io.javalin.core.plugin.Plugin;
import io.javalin.core.plugin.PluginAlreadyRegisteredException;
import io.javalin.core.plugin.PluginInitLifecycleViolationException;
import io.javalin.core.plugin.PluginLifecycleInit;
import io.javalin.core.plugin.PluginNotFoundException;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.SecurityUtil;
import io.javalin.core.util.CorsPlugin;
import io.javalin.core.util.Header;
import io.javalin.core.util.LogUtil;
import io.javalin.http.Handler;
import io.javalin.http.RequestLogger;
import io.javalin.http.SinglePageHandler;
import io.javalin.http.staticfiles.JettyResourceHandler;
import io.javalin.http.staticfiles.Location;
import io.javalin.http.staticfiles.ResourceHandler;
import io.javalin.http.staticfiles.StaticFileConfig;
import io.javalin.websocket.WsConfig;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JavalinConfig {
    // @formatter:off
    public boolean autogenerateEtags = false;
    public boolean prefer405over404 = false;
    public boolean enforceSsl = false;
    public boolean showJavalinBanner = true;
    public boolean logIfServerNotStarted = true;
    public boolean ignoreTrailingSlashes = true;
    @NotNull public String defaultContentType = "text/plain";
    @NotNull public String contextPath = "/";
    public Long maxRequestSize = 1_000_000L; // server will not accept payloads larger than 1mb by default
    @NotNull public Long asyncRequestTimeout = 0L;
    @NotNull public Inner inner = new Inner();

    // it's not bad to access this, the main reason it's hidden
    // is to provide a cleaner API with dedicated setters
    public static class Inner {
        @NotNull public Map<Class<? extends Plugin>, Plugin> plugins = new HashMap<>();
        @NotNull public Map<Class<?>, Object> appAttributes = new HashMap<>();
        @Nullable public RequestLogger requestLogger = null;
        @Nullable public ResourceHandler resourceHandler = null;
        @NotNull public AccessManager accessManager = SecurityUtil::noopAccessManager;
        @NotNull public SinglePageHandler singlePageHandler = new SinglePageHandler();
        @Nullable public SessionHandler sessionHandler = null;
        @Nullable public Consumer<WebSocketServletFactory> wsFactoryConfig = null;
        @Nullable public WsConfig wsLogger = null;
        @Nullable public Server server = null;
        @Nullable public Consumer<ServletContextHandler> servletContextHandlerConsumer = null;
        @NotNull public CompressionStrategy compressionStrategy = CompressionStrategy.GZIP;
    }
    // @formatter:on

    /**
     * Register a new plugin.
     */
    public JavalinConfig registerPlugin(@NotNull Plugin plugin) {
        if (inner.plugins.containsKey(plugin.getClass())) {
            throw new PluginAlreadyRegisteredException(plugin.getClass());
        }
        inner.plugins.put(plugin.getClass(), plugin);
        return this;
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

    public JavalinConfig enableDevLogging() {
        requestLogger(LogUtil::requestDevLogger);
        wsLogger(LogUtil::wsDevLogger);
        return this;
    }

    public JavalinConfig enableWebjars() {
        return addStaticFiles(staticFiles -> {
            staticFiles.directory = "META-INF/resources/webjars";
            staticFiles.headers.put(Header.CACHE_CONTROL, "max-age=31622400");
        });
    }

    public JavalinConfig addStaticFiles(@NotNull String directory, @NotNull Location location) {
        return addStaticFiles(staticFiles -> {
            staticFiles.directory = directory;
            staticFiles.location = location;
        });
    }

    public JavalinConfig addStaticFiles(@NotNull Consumer<StaticFileConfig> userConfig) {
        JettyUtil.disableJettyLogger();
        if (inner.resourceHandler == null) {
            inner.resourceHandler = new JettyResourceHandler();
        }
        StaticFileConfig finalConfig = new StaticFileConfig();
        userConfig.accept(finalConfig);
        inner.resourceHandler.addStaticFileConfig(finalConfig);
        return this;
    }

    public JavalinConfig addSinglePageRoot(@NotNull String hostedPath, @NotNull String filePath) {
        addSinglePageRoot(hostedPath, filePath, Location.CLASSPATH);
        return this;
    }

    public JavalinConfig addSinglePageRoot(@NotNull String hostedPath, @NotNull String filePath, @NotNull Location location) {
        inner.singlePageHandler.add(hostedPath, filePath, location);
        return this;
    }

    public JavalinConfig addSinglePageHandler(@NotNull String hostedPath, @NotNull Handler customHandler) {
        inner.singlePageHandler.add(hostedPath, customHandler);
        return this;
    }

    public JavalinConfig enableCorsForAllOrigins() {
        registerPlugin(CorsPlugin.forAllOrigins());
        return this;
    }

    public JavalinConfig enableCorsForOrigin(@NotNull String... origins) {
        registerPlugin(CorsPlugin.forOrigins(origins));
        return this;
    }

    public JavalinConfig accessManager(@NotNull AccessManager accessManager) {
        inner.accessManager = accessManager;
        return this;
    }

    public JavalinConfig requestLogger(@NotNull RequestLogger requestLogger) {
        inner.requestLogger = requestLogger;
        return this;
    }

    public JavalinConfig sessionHandler(@NotNull Supplier<SessionHandler> sessionHandlerSupplier) {
        JettyUtil.disableJettyLogger();
        inner.sessionHandler = sessionHandlerSupplier.get();
        return this;
    }

    public JavalinConfig wsFactoryConfig(@NotNull Consumer<WebSocketServletFactory> wsFactoryConfig) {
        inner.wsFactoryConfig = wsFactoryConfig;
        return this;
    }

    public JavalinConfig wsLogger(@NotNull Consumer<WsConfig> ws) {
        WsConfig logger = new WsConfig();
        ws.accept(logger);
        inner.wsLogger = logger;
        return this;
    }

    public JavalinConfig server(Supplier<Server> server) {
        inner.server = server.get();
        return this;
    }

    public JavalinConfig configureServletContextHandler(Consumer<ServletContextHandler> consumer) {
        inner.servletContextHandlerConsumer = consumer;
        return this;
    }

    public JavalinConfig compressionStrategy(Brotli brotli, Gzip gzip) {
        inner.compressionStrategy = new CompressionStrategy(brotli, gzip);
        return this;
    }

    public JavalinConfig compressionStrategy(CompressionStrategy compressionStrategy) {
        inner.compressionStrategy = compressionStrategy;
        return this;
    }

    public static void applyUserConfig(Javalin app, JavalinConfig config, Consumer<JavalinConfig> userConfig) {
        userConfig.accept(config); // apply user config to the default config

        AtomicBoolean anyHandlerAdded = new AtomicBoolean(false);
        app.events(listener -> {
            listener.handlerAdded(x -> anyHandlerAdded.set(true));
            listener.wsHandlerAdded(x -> anyHandlerAdded.set(true));
        });

        config.getPluginsExtending(PluginLifecycleInit.class)
            .forEach(plugin -> {
                plugin.init(app);
                if (anyHandlerAdded.get()) { // check if any "init" added a handler
                    throw new PluginInitLifecycleViolationException(((Plugin) plugin).getClass());
                }
            });

        config.inner.plugins.values().forEach(plugin -> plugin.apply(app));

        if (config.enforceSsl) {
            app.before(SecurityUtil::sslRedirect);
        }
    }

    private <T> Stream<? extends T> getPluginsExtending(Class<T> clazz) {
        return inner.plugins.values()
            .stream()
            .filter(clazz::isInstance)
            .map(plugin -> (T) plugin);
    }
}
