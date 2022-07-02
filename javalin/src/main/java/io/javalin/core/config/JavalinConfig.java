/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.config;

import io.javalin.Javalin;
import io.javalin.core.plugin.Plugin;
import io.javalin.core.plugin.PluginAlreadyRegisteredException;
import io.javalin.core.plugin.PluginUtil;
import io.javalin.core.security.AccessManager;
import io.javalin.core.security.SecurityUtil;
import io.javalin.core.util.ConcurrencyUtil;
import io.javalin.core.validation.JavalinValidation;
import io.javalin.http.ContentType;
import io.javalin.http.ContextResolver;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.json.JsonMapper;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import static io.javalin.http.ContextKt.ASYNC_EXECUTOR_KEY;
import static io.javalin.http.ContextResolverKt.CONTEXT_RESOLVER_KEY;
import static io.javalin.http.util.ContextUtil.MAX_REQUEST_SIZE_KEY;
import static io.javalin.plugin.json.JsonMapperKt.JSON_MAPPER_KEY;

public class JavalinConfig {
    public boolean autogenerateEtags = false;
    public boolean prefer405over404 = false;
    public boolean showJavalinBanner = true;
    public Long maxRequestSize = 1_000_000L; // increase this or use inputstream to handle large requests
    @NotNull public String defaultContentType = ContentType.PLAIN;
    @NotNull public Long asyncRequestTimeout = 0L;
    public InnerConfig inner = new InnerConfig();
    public RoutingConfig routing = new RoutingConfig();
    public JettyConfig jetty = new JettyConfig(inner);
    public StaticFilesConfig staticFiles = new StaticFilesConfig(inner);
    public SinglePageConfig singlePage = new SinglePageConfig(inner);
    public CompressionConfig compression = new CompressionConfig(inner);
    public LoggingConfig requestLoggers = new LoggingConfig(inner);
    public PluginConfig plugins = new PluginConfig(inner);

    public void contextResolvers(@NotNull Consumer<ContextResolver> userResolver) {
        ContextResolver finalResolver = new ContextResolver();
        userResolver.accept(finalResolver);
        inner.appAttributes.put(CONTEXT_RESOLVER_KEY, finalResolver);
    }

    public void accessManager(@NotNull AccessManager accessManager) {
        inner.accessManager = accessManager;
    }

    public void jsonMapper(JsonMapper jsonMapper) {
        inner.appAttributes.put(JSON_MAPPER_KEY, jsonMapper);
    }

    public static void applyUserConfig(Javalin app, JavalinConfig config, Consumer<JavalinConfig> userConfig) {
        JavalinValidation.addValidationExceptionMapper(app); // add default mapper for validation
        userConfig.accept(config); // apply user config to the default config
        PluginUtil.attachPlugins(app, config.inner.plugins.values());
        config.inner.appAttributes.putIfAbsent(JSON_MAPPER_KEY, new JavalinJackson());
        config.inner.appAttributes.putIfAbsent(CONTEXT_RESOLVER_KEY, new ContextResolver());
        config.inner.appAttributes.putIfAbsent(ASYNC_EXECUTOR_KEY, ConcurrencyUtil.executorService("JavalinDefaultAsyncThreadPool"));
        config.inner.appAttributes.putIfAbsent(MAX_REQUEST_SIZE_KEY, config.maxRequestSize);
    }

}
