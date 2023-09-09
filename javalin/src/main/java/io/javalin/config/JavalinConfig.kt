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
import io.javalin.plugin.JavalinPlugin
import io.javalin.plugin.PluginConfiguration
import io.javalin.plugin.PluginFactory
import io.javalin.rendering.FILE_RENDERER_KEY
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.NotImplementedRenderer
import io.javalin.security.AccessManager
import io.javalin.validation.JavalinValidation.addValidationExceptionMapper
import io.javalin.vue.JAVALINVUE_CONFIG_KEY
import io.javalin.vue.JavalinVueConfig
import java.util.function.Consumer

// this class should be abbreviated `cfg` in the source code.
// `cfg.pvt` should be accessible, but usage should be discouraged (hence the naming)
class JavalinConfig {
    //@formatter:off
    @JvmField val http = HttpConfig()
    @JvmField val router = RouterConfig(this)
    @JvmField val jetty = JettyConfig(this)
    @JvmField val staticFiles = StaticFilesConfig(this)
    @JvmField val spaRoot = SpaRootConfig(this)
    @JvmField val compression = CompressionConfig(this)
    @JvmField val requestLogger = RequestLoggerConfig(this)
    @JvmField val bundledPlugins = BundledPluginsConfig(this)
    @JvmField val events = EventConfig(this)
    @JvmField val vue = JavalinVueConfig()
    @JvmField val contextResolver = ContextResolverConfig()
    @JvmField var showJavalinBanner = true
    /**
     * This is "private", only use it if you know what you're doing
     */
    @JvmField val pvt = PrivateConfig(this)

    fun events(listener:Consumer<EventConfig>) { listener.accept(this.events) }
    fun accessManager(accessManager: AccessManager) { pvt.accessManager = accessManager }
    fun jsonMapper(jsonMapper: JsonMapper) { pvt.appAttributes[JSON_MAPPER_KEY] = jsonMapper }
    fun fileRenderer(fileRenderer: FileRenderer) { pvt.appAttributes[FILE_RENDERER_KEY] = fileRenderer }
    //@formatter:on

    companion object {
        @JvmStatic
        fun applyUserConfig(app: Javalin, cfg: JavalinConfig, userConfig: Consumer<JavalinConfig>) {
            userConfig.accept(cfg) // apply user config to the default config
            cfg.pvt.pluginManager.startPlugins(app)
            cfg.pvt.appAttributes.computeIfAbsent(JSON_MAPPER_KEY) { JavalinJackson() }
            cfg.pvt.appAttributes.computeIfAbsent(FILE_RENDERER_KEY) { NotImplementedRenderer() }
            cfg.pvt.appAttributes.computeIfAbsent(CONTEXT_RESOLVER_KEY) { cfg.contextResolver }
            cfg.pvt.appAttributes.computeIfAbsent(MAX_REQUEST_SIZE_KEY) { cfg.http.maxRequestSize }
            cfg.pvt.appAttributes.computeIfAbsent(JAVALINVUE_CONFIG_KEY) { cfg.vue }
            addValidationExceptionMapper(app) // add default mapper for validation
        }
    }

    fun registerPlugin(plugin: JavalinPlugin): JavalinConfig = also {
        pvt.pluginManager.register(plugin)
    }

    @JvmOverloads
    fun <PLUGIN : JavalinPlugin, CFG : PluginConfiguration> registerPlugin(factory: PluginFactory<PLUGIN, CFG>, cfg: Consumer<CFG> = Consumer {}) =
        registerPlugin(factory.create(cfg))

}
