/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.core.config;

import io.javalin.Javalin;
import io.javalin.core.plugin.PluginUtil;
import io.javalin.core.security.AccessManager;
import io.javalin.core.util.ConcurrencyUtil;
import io.javalin.core.validation.JavalinValidation;
import io.javalin.http.ContextResolver;
import io.javalin.plugin.json.JavalinJackson;
import io.javalin.plugin.json.JsonMapper;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import static io.javalin.http.ContextKt.ASYNC_EXECUTOR_KEY;
import static io.javalin.http.ContextResolverKt.CONTEXT_RESOLVER_KEY;
import static io.javalin.http.util.ContextUtil.MAX_REQUEST_SIZE_KEY;
import static io.javalin.plugin.json.JsonMapperKt.JSON_MAPPER_KEY;

public class JavalinConfig { // abbreviated `cfg` in source
    public boolean showJavalinBanner = true;
    public HttpConfig http = new HttpConfig();
    public RoutingConfig routing = new RoutingConfig();
    public PrivateConfig pvt = new PrivateConfig(); // this is "private", only use it if you know what you're doing
    public JettyConfig jetty = new JettyConfig(pvt);
    public StaticFilesConfig staticFiles = new StaticFilesConfig(pvt);
    public SinglePageConfig singlePage = new SinglePageConfig(pvt);
    public CompressionConfig compression = new CompressionConfig(pvt);
    public LoggingConfig requestLoggers = new LoggingConfig(pvt);
    public PluginConfig plugins = new PluginConfig(pvt);

    public void contextResolvers(@NotNull Consumer<ContextResolver> userResolver) {
        ContextResolver finalResolver = new ContextResolver();
        userResolver.accept(finalResolver);
        pvt.appAttributes.put(CONTEXT_RESOLVER_KEY, finalResolver);
    }

    public void accessManager(@NotNull AccessManager accessManager) {
        pvt.accessManager = accessManager;
    }

    public void jsonMapper(@NotNull JsonMapper jsonMapper) {
        pvt.appAttributes.put(JSON_MAPPER_KEY, jsonMapper);
    }

    public static void applyUserConfig(Javalin app, JavalinConfig cfg, Consumer<JavalinConfig> userConfig) {
        JavalinValidation.addValidationExceptionMapper(app); // add default mapper for validation
        userConfig.accept(cfg); // apply user config to the default config
        PluginUtil.attachPlugins(app, cfg.pvt.plugins.values());
        cfg.pvt.appAttributes.putIfAbsent(JSON_MAPPER_KEY, new JavalinJackson());
        cfg.pvt.appAttributes.putIfAbsent(CONTEXT_RESOLVER_KEY, new ContextResolver());
        cfg.pvt.appAttributes.putIfAbsent(ASYNC_EXECUTOR_KEY, ConcurrencyUtil.executorService("JavalinDefaultAsyncThreadPool"));
        cfg.pvt.appAttributes.putIfAbsent(MAX_REQUEST_SIZE_KEY, cfg.http.maxRequestSize);
    }

}
