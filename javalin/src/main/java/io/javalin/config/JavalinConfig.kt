/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.config

import io.javalin.Javalin
import io.javalin.http.servlet.MAX_REQUEST_SIZE_KEY
import io.javalin.json.JSON_MAPPER_KEY
import io.javalin.json.JavalinJackson
import io.javalin.json.JsonMapper
import io.javalin.plugin.PluginUtil.attachPlugins
import io.javalin.security.AccessManager
import io.javalin.validation.JavalinValidation.addValidationExceptionMapper
import io.javalin.vue.JAVALINVUE_CONFIG_KEY
import io.javalin.vue.JavalinVueConfig
import java.util.function.Consumer

// this class should be abbreviated `cfg` in the source code.
// `cfg.pvt` should be accessible, but usage should be discouraged (hence the naming)
class JavalinConfig {
    //@formatter:off
    @JvmField val pvt = PrivateConfig() // this is "private", only use it if you know what you're doing
    @JvmField val http = HttpConfig()
    @JvmField val routing = RoutingConfig()
    @JvmField val jetty = JettyConfig(pvt)
    @JvmField val staticFiles = StaticFilesConfig(pvt)
    @JvmField val spaRoot = SpaRootConfig(pvt)
    @JvmField val compression = CompressionConfig(pvt)
    @JvmField val requestLogger = RequestLoggerConfig(pvt)
    @JvmField val plugins = PluginConfig(pvt)
    @JvmField val vue = JavalinVueConfig()
    @JvmField val contextResolver = ContextResolverConfig()
    @JvmField var showJavalinBanner = true
    fun accessManager(accessManager: AccessManager) { pvt.accessManager = accessManager }
    fun jsonMapper(jsonMapper: JsonMapper) { pvt.appAttributes[JSON_MAPPER_KEY] = jsonMapper }
    //@formatter:on
    companion object {
        @JvmStatic
        fun applyUserConfig(app: Javalin, cfg: JavalinConfig, userConfig: Consumer<JavalinConfig>) {
            addValidationExceptionMapper(app) // add default mapper for validation
            userConfig.accept(cfg) // apply user config to the default config
            attachPlugins(app, cfg.pvt.plugins.values)
            cfg.pvt.appAttributes.putIfAbsent(JSON_MAPPER_KEY, JavalinJackson())
            cfg.pvt.appAttributes.putIfAbsent(CONTEXT_RESOLVER_KEY, cfg.contextResolver)
            cfg.pvt.appAttributes.putIfAbsent(MAX_REQUEST_SIZE_KEY, cfg.http.maxRequestSize)
            cfg.pvt.appAttributes.putIfAbsent(JAVALINVUE_CONFIG_KEY, cfg.vue)
        }
    }
}
