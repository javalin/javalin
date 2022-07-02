/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.config;

import io.javalin.Javalin;
import io.javalin.core.compression.Brotli;
import io.javalin.core.compression.CompressionStrategy;
import io.javalin.core.compression.Gzip;
import io.javalin.core.plugin.Plugin;
import io.javalin.core.plugin.PluginAlreadyRegisteredException;
import io.javalin.core.plugin.PluginUtil;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.SecurityUtil;
import io.javalin.core.util.ConcurrencyUtil;
import io.javalin.core.validation.JavalinValidation;
import io.javalin.http.ContentType;
import io.javalin.http.ContextResolver;
import io.javalin.http.RequestLogger;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.json.JsonMapper;
import io.javalin.websocket.WsConfig;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import static io.javalin.http.ContextKt.ASYNC_EXECUTOR_KEY;
import static io.javalin.http.ContextResolverKt.CONTEXT_RESOLVER_KEY;
import static io.javalin.http.util.ContextUtil.MAX_REQUEST_SIZE_KEY;
import static io.javalin.plugin.json.JsonMapperKt.JSON_MAPPER_KEY;

public class JavalinConfig {
    // @formatter:off
    public boolean autogenerateEtags = false;
    public boolean prefer405over404 = false;
    public boolean enforceSsl = false;
    public boolean showJavalinBanner = true;
    @NotNull public String defaultContentType = ContentType.PLAIN;
    public Long maxRequestSize = 1_000_000L; // either increase this or use inputstream to handle large requests
    @NotNull public Long asyncRequestTimeout = 0L;
    @NotNull public RoutingConfig routing = new RoutingConfig();
    @NotNull public InnerConfig inner = new InnerConfig();
    public JettyConfig jetty = new JettyConfig(inner);
    public StaticFilesConfig staticFiles = new StaticFilesConfig(inner);
    public SinglePageConfig singlePage = new SinglePageConfig(inner);
    public DefaultPluginConfig defaultPlugins = new DefaultPluginConfig(this);

    public void registerPlugin(@NotNull Plugin plugin) {
        if (inner.plugins.containsKey(plugin.getClass())) {
            throw new PluginAlreadyRegisteredException(plugin.getClass());
        }
        inner.plugins.put(plugin.getClass(), plugin);
    }

    public void contextResolvers(@NotNull Consumer<ContextResolver> userResolver) {
        ContextResolver finalResolver = new ContextResolver();
        userResolver.accept(finalResolver);
        inner.appAttributes.put(CONTEXT_RESOLVER_KEY, finalResolver);
    }

    public void accessManager(@NotNull AccessManager accessManager) {
        inner.accessManager = accessManager;
    }

    public void requestLogger(@NotNull RequestLogger requestLogger) {
        inner.requestLogger = requestLogger;
    }

    public void wsLogger(@NotNull Consumer<WsConfig> ws) {
        WsConfig logger = new WsConfig();
        ws.accept(logger);
        inner.wsLogger = logger;
    }

    public void compressionStrategy(Brotli brotli, Gzip gzip) {
        inner.compressionStrategy = new CompressionStrategy(brotli, gzip);
    }

    public void compressionStrategy(CompressionStrategy compressionStrategy) {
        inner.compressionStrategy = compressionStrategy;
    }

    public void jsonMapper(JsonMapper jsonMapper) {
        inner.appAttributes.put(JSON_MAPPER_KEY, jsonMapper);
    }

    public static void applyUserConfig(Javalin app, JavalinConfig config, Consumer<JavalinConfig> userConfig) {
        JavalinValidation.addValidationExceptionMapper(app); // add default mapper for validation
        userConfig.accept(config); // apply user config to the default config
        if (config.enforceSsl) {
            app.before(SecurityUtil::sslRedirect); // needs to be the first handler
        }
        PluginUtil.attachPlugins(app, config.inner.plugins.values());
        config.inner.appAttributes.putIfAbsent(JSON_MAPPER_KEY, new JavalinJackson());
        config.inner.appAttributes.putIfAbsent(CONTEXT_RESOLVER_KEY, new ContextResolver());
        config.inner.appAttributes.putIfAbsent(ASYNC_EXECUTOR_KEY, ConcurrencyUtil.executorService("JavalinDefaultAsyncThreadPool"));
        config.inner.appAttributes.putIfAbsent(MAX_REQUEST_SIZE_KEY, config.maxRequestSize);
    }

}
