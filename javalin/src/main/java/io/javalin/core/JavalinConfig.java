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
import io.javalin.core.util.Headers;
import io.javalin.core.util.HeadersPlugin;
import io.javalin.core.util.HttpAllowedMethodsOnRoutesUtil;
import io.javalin.core.util.LogUtil;
import io.javalin.http.ContentType;
import io.javalin.http.ContextResolver;
import io.javalin.http.Handler;
import io.javalin.http.RequestLogger;
import io.javalin.http.SinglePageHandler;
import io.javalin.http.staticfiles.Location;
import io.javalin.http.staticfiles.ResourceHandler;
import io.javalin.http.staticfiles.StaticFileConfig;
import io.javalin.jetty.JettyResourceHandler;
import io.javalin.jetty.JettyUtil;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.json.JsonMapper;
import io.javalin.websocket.WsConfig;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.server.JettyWebSocketServletFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static io.javalin.http.ContextResolverKt.CONTEXT_RESOLVER_KEY;
import static io.javalin.http.util.ContextUtil.maxRequestSizeKey;
import static io.javalin.plugin.json.JsonMapperKt.JSON_MAPPER_KEY;

public class JavalinConfig {
    // @formatter:off
    public boolean autogenerateEtags = false;
    public boolean prefer405over404 = false;
    public boolean enforceSsl = false;
    public boolean showJavalinBanner = true;
    @NotNull public String defaultContentType = ContentType.PLAIN;
    @NotNull public String contextPath = "/";
    public Long maxRequestSize = 1_000_000L; // either increase this or use inputstream to handle large requests
    @NotNull public Long asyncRequestTimeout = 0L;
    @NotNull public RoutingConfig routing = new RoutingConfig();
    @NotNull public Inner inner = new Inner();

    public static class RoutingConfig {
        public boolean ignoreTrailingSlashes = true;
        public boolean treatMultipleSlashesAsSingleSlash = false;
    }

    // it's not bad to access this, the main reason it's hidden
    // is to provide a cleaner API with dedicated setters
    public static class Inner {
        @NotNull public Map<Class<? extends Plugin>, Plugin> plugins = new LinkedHashMap<>();
        @NotNull public Map<String, Object> appAttributes = new HashMap<>();
        @Nullable public RequestLogger requestLogger = null;
        @Nullable public ResourceHandler resourceHandler = null;
        @NotNull public AccessManager accessManager = SecurityUtil::noopAccessManager;
        @NotNull public SinglePageHandler singlePageHandler = new SinglePageHandler();
        @Nullable public SessionHandler sessionHandler = null;
        @Nullable public Consumer<JettyWebSocketServletFactory> wsFactoryConfig = null;
        @Nullable public WsConfig wsLogger = null;
        @Nullable public Server server = null;
        @Nullable public Consumer<ServletContextHandler> servletContextHandlerConsumer = null;
        @NotNull public CompressionStrategy compressionStrategy = CompressionStrategy.GZIP;
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
        registerPlugin(new LogUtil.HandlerLoggingPlugin());
    }

    public void enableWebjars() {
        addStaticFiles(staticFiles -> {
            staticFiles.directory = "META-INF/resources/webjars";
            staticFiles.headers.put(Header.CACHE_CONTROL, "max-age=31622400");
        });
    }

    public void addStaticFiles(@NotNull String directory, @NotNull Location location) {
        addStaticFiles(staticFiles -> {
            staticFiles.directory = directory;
            staticFiles.location = location;
        });
    }

    public void addStaticFiles(@NotNull Consumer<StaticFileConfig> userConfig) {
        if (inner.resourceHandler == null) {
            inner.resourceHandler = new JettyResourceHandler();
        }
        StaticFileConfig finalConfig = new StaticFileConfig();
        userConfig.accept(finalConfig);
        inner.resourceHandler.addStaticFileConfig(finalConfig);
    }

    public void addSinglePageRoot(@NotNull String hostedPath, @NotNull String filePath) {
        addSinglePageRoot(hostedPath, filePath, Location.CLASSPATH);
    }

    public void addSinglePageRoot(@NotNull String hostedPath, @NotNull String filePath, @NotNull Location location) {
        inner.singlePageHandler.add(hostedPath, filePath, location);
    }

    public void addSinglePageHandler(@NotNull String hostedPath, @NotNull Handler customHandler) {
        inner.singlePageHandler.add(hostedPath, customHandler);
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
        inner.sessionHandler = sessionHandlerSupplier.get();
    }

    public void wsFactoryConfig(@NotNull Consumer<JettyWebSocketServletFactory> wsFactoryConfig) {
        inner.wsFactoryConfig = wsFactoryConfig;
    }

    public void wsLogger(@NotNull Consumer<WsConfig> ws) {
        WsConfig logger = new WsConfig();
        ws.accept(logger);
        inner.wsLogger = logger;
    }

    public void server(Supplier<Server> server) {
        inner.server = server.get();
    }

    public void configureServletContextHandler(Consumer<ServletContextHandler> consumer) {
        inner.servletContextHandlerConsumer = consumer;
    }

    public void compressionStrategy(Brotli brotli, Gzip gzip) {
        inner.compressionStrategy = new CompressionStrategy(brotli, gzip);
    }

    public void compressionStrategy(CompressionStrategy compressionStrategy) {
        inner.compressionStrategy = compressionStrategy;
    }

    public void globalHeaders(Supplier<Headers> headers) {
        registerPlugin(new HeadersPlugin(headers.get()));
    }

    public void jsonMapper(JsonMapper jsonMapper) {
        inner.appAttributes.put(JSON_MAPPER_KEY, jsonMapper);
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
        config.inner.appAttributes.putIfAbsent(JSON_MAPPER_KEY, new JavalinJackson());
        app.attribute(maxRequestSizeKey, config.maxRequestSize);

        config.inner.appAttributes.putIfAbsent(CONTEXT_RESOLVER_KEY, new ContextResolver());
    }

    private <T> Stream<? extends T> getPluginsExtending(Class<T> clazz) {
        return inner.plugins.values()
            .stream()
            .filter(clazz::isInstance)
            .map(plugin -> (T) plugin);
    }

    public void contextResolvers(@NotNull Consumer<ContextResolver> userResolver) {
        ContextResolver finalResolver = new ContextResolver();
        userResolver.accept(finalResolver);
        inner.appAttributes.put(CONTEXT_RESOLVER_KEY, finalResolver);
    }

    public void enableHttpAllowedMethodsOnRoutes() {
        registerPlugin(new HttpAllowedMethodsOnRoutesUtil());
    }
}
