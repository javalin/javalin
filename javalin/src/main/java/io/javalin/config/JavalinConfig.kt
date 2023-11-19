/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.config

import io.javalin.Javalin
import io.javalin.component.ComponentAccessor
import io.javalin.component.ComponentResolver
import io.javalin.component.ConfigurableComponentAccessor
import io.javalin.component.ConfigurableComponentResolver
import io.javalin.config.ContextResolverConfig.Companion.CONTEXT_RESOLVER
import io.javalin.http.servlet.MAX_REQUEST_SIZE
import io.javalin.http.util.AsyncExecutor.Companion.ASYNC_EXECUTOR
import io.javalin.json.JavalinJackson
import io.javalin.json.JsonMapper
import io.javalin.plugin.Plugin
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.FileRenderer.Companion.FILE_RENDERER
import io.javalin.rendering.NotImplementedRenderer
import io.javalin.validation.Validation
import io.javalin.validation.Validation.Companion.VALIDATION
import io.javalin.validation.Validation.Companion.addValidationExceptionMapper
import io.javalin.vue.JavalinVueConfig
import io.javalin.vue.JavalinVueConfig.Companion.VUE_CONFIG
import java.util.function.Consumer

// this class should be abbreviated `cfg` in the source code.
// `cfg.pvt` should be accessible, but usage should be discouraged (hence the naming)
/**
 * Javalin configuration class.
 * @see [Javalin.create]
 */
class JavalinConfig {
    //@formatter:off
    /** The http layer configuration: etags, request size, timeout, etc */
    @JvmField val http = HttpConfig(this)
    /** The routing configuration: context path, slash treatment, etc */
    @JvmField val router = RouterConfig(this)
    /** The embedded Jetty webserver configuration */
    @JvmField val jetty = JettyConfig(this)
    /** Static files and webjars configuration */
    @JvmField val staticFiles = StaticFilesConfig(this)
    /** Single Page Application roots configuration */
    @JvmField val spaRoot = SpaRootConfig(this)
    /** Request Logger configuration: http and websocket loggers */
    @JvmField val requestLogger = RequestLoggerConfig(this)
    /** Bundled plugins configuration: enable bundled plugins or add custom ones */
    @JvmField val bundledPlugins = BundledPluginsConfig(this)
    /** Events configuration */
    @JvmField val events = EventConfig(this)
    /** Vue Plugin configuration */
    @JvmField val vue = JavalinVueConfig()
    /** Context resolver implementation configuration */
    @JvmField val contextResolver = ContextResolverConfig()
    /** Use virtual threads (based on Java Project Loom) */
    @JvmField var useVirtualThreads = false
    /** Show the Javalin banner in the logs */
    @JvmField var showJavalinBanner = true
    @JvmField var validation = ValidationConfig()
    /**
     * By default, Javalin will print a warning after 5s if you create a Javalin instance without starting it.
     * You can disable this behavior by setting this to false.
     */
    @JvmField var startupWatcherEnabled = true
    /** This is "private", only use it if you know what you're doing */
    @JvmField val pvt = PrivateConfig(this)

    /**
     * Adds an event listener to this Javalin Configuration.
     * @see [EventConfig]
     */
    fun events(listener:Consumer<EventConfig>) { listener.accept(this.events) }

    /**
     * Sets the [JsonMapper] to be used in this Javalin Configuration.
     * @param jsonMapper the [JsonMapper]
     */
    fun jsonMapper(jsonMapper: JsonMapper) { pvt.jsonMapper = jsonMapper }

    /**
     * Sets the [FileRenderer] to be used in this Javalin Configuration.
     * @param fileRenderer the [FileRenderer]
     */
    fun fileRenderer(fileRenderer: FileRenderer) { registerComponent(FILE_RENDERER) { fileRenderer } }
    //@formatter:on

    companion object {
        @JvmStatic
        fun applyUserConfig(cfg: JavalinConfig, userConfig: Consumer<JavalinConfig>) {
            addValidationExceptionMapper(cfg) // add default mapper for validation
            userConfig.accept(cfg) // apply user config to the default config
            cfg.pvt.pluginManager.startPlugins()
            cfg.pvt.componentManager.registerIfAbsent(ASYNC_EXECUTOR) { cfg.pvt.asyncExecutor.value }
            val validation = Validation(cfg.validation)
            cfg.pvt.componentManager.registerIfAbsent(VALIDATION) { validation }
            if (cfg.pvt.jsonMapper == null) { cfg.pvt.jsonMapper = JavalinJackson(null, cfg.useVirtualThreads) }
            cfg.pvt.componentManager.registerIfAbsent(FILE_RENDERER) { NotImplementedRenderer() }
            cfg.pvt.componentManager.registerIfAbsent(CONTEXT_RESOLVER) { cfg.contextResolver }
            cfg.pvt.componentManager.registerIfAbsent(MAX_REQUEST_SIZE) { cfg.http.maxRequestSize }
            cfg.pvt.componentManager.registerIfAbsent(VUE_CONFIG) { cfg.vue }
        }
    }

    fun <COMPONENT : Any?> registerComponent(key: ComponentAccessor<COMPONENT>, resolver: ComponentResolver<COMPONENT>) {
        pvt.componentManager.registerResolver(key, resolver)
    }

    fun <COMPONENT : Any?, CFG> registerComponent(key: ConfigurableComponentAccessor<COMPONENT, CFG>, resolver: ConfigurableComponentResolver<COMPONENT, CFG>) {
        pvt.componentManager.registerResolver(key, resolver)
    }

    /**
     * Register a plugin to this Javalin Configuration.
     * @param CFG the type of the configuration class for the plugin
     * @param plugin the [Plugin] to register
     */
    fun <CFG> registerPlugin(plugin: Plugin<CFG>): Plugin<CFG> =
        plugin.also { pvt.pluginManager.register(plugin) }

}
