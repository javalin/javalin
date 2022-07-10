/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.config

import io.javalin.Javalin
import io.javalin.http.ASYNC_EXECUTOR_KEY
import io.javalin.http.util.ContextUtil.MAX_REQUEST_SIZE_KEY
import io.javalin.plugin.PluginUtil.attachPlugins
import io.javalin.plugin.json.JSON_MAPPER_KEY
import io.javalin.plugin.json.JavalinJackson
import io.javalin.util.ConcurrencyUtil.executorService
import io.javalin.validation.JavalinValidation.addValidationExceptionMapper
import java.util.function.Consumer

// this class should be abbreviated `cfg` in the source code.
// `cfg.pvt` should be accessible, but usage should be discouraged (hence the naming)
class JavalinConfig {
    //@formatter:off
    @JvmField val pvt = PrivateConfig() // this is "private", only use it if you know what you're doing
    @JvmField val core = CoreConfig(pvt)
    @JvmField val http = HttpConfig()
    @JvmField val routing = RoutingConfig()
    @JvmField val jetty = JettyConfig(pvt)
    @JvmField val staticFiles = StaticFilesConfig(pvt)
    @JvmField val spaRoot = SpaRootConfig(pvt)
    @JvmField val compression = CompressionConfig(pvt)
    @JvmField val requestLoggers = LoggingConfig(pvt)
    @JvmField val plugins = PluginConfig(pvt)
    //@formatter:on
    companion object {
        @JvmStatic
        fun applyUserConfig(app: Javalin, cfg: JavalinConfig, userConfig: Consumer<JavalinConfig>) {
            addValidationExceptionMapper(app) // add default mapper for validation
            userConfig.accept(cfg) // apply user config to the default config
            attachPlugins(app, cfg.pvt.plugins.values)
            cfg.pvt.appAttributes.putIfAbsent(JSON_MAPPER_KEY, JavalinJackson())
            cfg.pvt.appAttributes.putIfAbsent(CONTEXT_RESOLVER_KEY, ContextResolver())
            cfg.pvt.appAttributes.putIfAbsent(ASYNC_EXECUTOR_KEY, executorService("JavalinDefaultAsyncThreadPool"))
            cfg.pvt.appAttributes.putIfAbsent(MAX_REQUEST_SIZE_KEY, cfg.http.maxRequestSize)
        }
    }
}
