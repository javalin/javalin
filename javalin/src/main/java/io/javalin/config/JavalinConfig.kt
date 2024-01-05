/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.config

import io.javalin.Javalin
import io.javalin.config.ContextResolverConfig.Companion.ContextResolverKey
import io.javalin.http.servlet.MaxRequestSize.MaxRequestSizeKey
import io.javalin.http.util.AsyncExecutor.Companion.AsyncExecutorKey
import io.javalin.jetty.JettyServer
import io.javalin.json.JsonMapper
import io.javalin.plugin.Plugin
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.FileRenderer.Companion.FileRendererKey
import io.javalin.rendering.NotImplementedRenderer
import io.javalin.util.javalinLazy
import io.javalin.validation.Validation
import io.javalin.validation.Validation.Companion.ValidationKey
import io.javalin.validation.Validation.Companion.addValidationExceptionMapper
import io.javalin.vue.JavalinVueConfig
import io.javalin.vue.JavalinVueConfig.Companion.VueConfigKey
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
    /** Default validator configuration */
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
    fun jsonMapper(jsonMapper: JsonMapper) { pvt.jsonMapper = javalinLazy { jsonMapper } }

    /**
     * Sets the [FileRenderer] to be used in this Javalin Configuration.
     * @param fileRenderer the [FileRenderer]
     */
    fun fileRenderer(fileRenderer: FileRenderer) =
        appData(FileRendererKey, fileRenderer)

    /**
     * Register a plugin to this Javalin Configuration.
     * @param CFG the type of the configuration class for the plugin
     * @param plugin the [Plugin] to register
     */
    fun <CFG> registerPlugin(plugin: Plugin<CFG>): Plugin<CFG> =
        plugin.also { pvt.pluginManager.register(plugin) }

    /**
     * Register a new component resolver.
     * @param T the type of the value
     * @param key the [Key] to register
     */
    fun <T : Any?> appData(key: Key<T>, value: T) = pvt.appDataManager.register(key, value)


    companion object {
        @JvmStatic
        fun applyUserConfig(cfg: JavalinConfig, userConfig: Consumer<JavalinConfig>) {
            addValidationExceptionMapper(cfg) // add default mapper for validation
            userConfig.accept(cfg) // apply user config to the default config
            cfg.jetty.threadPool = cfg.jetty.threadPool ?: JettyServer.defaultThreadPool(cfg.useVirtualThreads)
            cfg.pvt.pluginManager.startPlugins()
            cfg.pvt.appDataManager.registerIfAbsent(ContextResolverKey, cfg.contextResolver)
            cfg.pvt.appDataManager.registerIfAbsent(AsyncExecutorKey, cfg.pvt.asyncExecutor.value)
            cfg.pvt.appDataManager.registerIfAbsent(ValidationKey, Validation(cfg.validation))
            cfg.pvt.appDataManager.registerIfAbsent(FileRendererKey, NotImplementedRenderer())
            cfg.pvt.appDataManager.registerIfAbsent(MaxRequestSizeKey, cfg.http.maxRequestSize)
            cfg.pvt.appDataManager.registerIfAbsent(VueConfigKey, cfg.vue)
        }
    }
    //@formatter:on

}
