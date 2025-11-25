/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.config

import io.javalin.Javalin
import io.javalin.compression.CompressionStrategy
import io.javalin.config.ContextResolverConfig.Companion.ContextResolverKey
import io.javalin.event.EventManager
import io.javalin.http.RequestLogger
import io.javalin.http.SinglePageHandler
import io.javalin.http.servlet.DefaultTasks
import io.javalin.http.servlet.JavalinServlet
import io.javalin.http.servlet.MaxRequestSize.MaxRequestSizeKey
import io.javalin.http.servlet.ServletEntry
import io.javalin.http.staticfiles.ResourceHandler
import io.javalin.http.util.AsyncExecutor
import io.javalin.http.util.AsyncExecutor.Companion.AsyncExecutorKey
import io.javalin.jetty.JettyUtil.createJettyServletWithWebsocketsIfAvailable
import io.javalin.json.JavalinJackson
import io.javalin.json.JsonMapper
import io.javalin.plugin.Plugin
import io.javalin.plugin.PluginManager
import io.javalin.rendering.FileRenderer
import io.javalin.rendering.FileRenderer.Companion.FileRendererKey
import io.javalin.rendering.NotImplementedRenderer
import io.javalin.router.InternalRouter
import io.javalin.router.exception.JavaLangErrorHandler
import io.javalin.util.ConcurrencyUtil
import io.javalin.util.javalinLazy
import io.javalin.validation.Validation
import io.javalin.validation.Validation.Companion.ValidationKey
import io.javalin.validation.Validation.Companion.addValidationExceptionMapper
import io.javalin.websocket.WsConfig
import io.javalin.websocket.WsRouter
import java.util.function.Consumer

/**
 * Javalin configuration class.
 * @see [Javalin.create]
 */
class JavalinState {
    //@formatter:off
    // PUBLIC CONFIG API
    @JvmField val http = HttpConfig()
    @JvmField val router = RouterConfig()
    @JvmField val contextResolver = ContextResolverConfig()
    @JvmField var validation = ValidationConfig()
    @JvmField val routes = RoutesConfig(this)
    @JvmField val jetty = JettyConfig(this)
    @JvmField val staticFiles = StaticFilesConfig(this)
    @JvmField val spaRoot = SpaRootConfig(this)
    @JvmField val requestLogger = RequestLoggerConfig(this)
    @JvmField val bundledPlugins = BundledPluginsConfig(this)
    @JvmField val events = EventConfig(this)
    @JvmField var useVirtualThreads = false
    @JvmField var showJavalinBanner = true
    @JvmField var showOldJavalinVersionWarning = true
    @JvmField var startupWatcherEnabled = true

    // INTERNAL CONFIG API
    @JvmField val eventManager = EventManager()
    @JvmField val wsRouter = WsRouter(router)
    @JvmField var internalRouter = InternalRouter(wsRouter, eventManager, router, jetty)
    @JvmField var jsonMapper: Lazy<JsonMapper> = javalinLazy { JavalinJackson(null, useVirtualThreads) }
    @JvmField var appDataManager = AppDataManager()
    @JvmField var pluginManager = PluginManager(this)
    @JvmField var httpRequestLogger: RequestLogger? = null
    @JvmField var wsRequestLogger: WsConfig? = null
    @JvmField var resourceHandler: ResourceHandler? = null
    @JvmField var singlePageHandler = SinglePageHandler()
    @JvmField var asyncExecutor = javalinLazy { AsyncExecutor(ConcurrencyUtil.executorService("JavalinDefaultAsyncThreadPool", useVirtualThreads)) }
    @JvmField var servletRequestLifecycle = mutableListOf(DefaultTasks.BEFORE, DefaultTasks.BEFORE_MATCHED, DefaultTasks.HTTP, DefaultTasks.AFTER_MATCHED, DefaultTasks.ERROR, DefaultTasks.AFTER)
    @JvmField var servlet: Lazy<ServletEntry> = javalinLazy { createJettyServletWithWebsocketsIfAvailable(this) ?: ServletEntry(servlet = JavalinServlet(this)) }
    @JvmField var jettyInternal = JettyInternalConfig()

    fun events(listener:Consumer<EventConfig>) { listener.accept(this.events) }
    fun jsonMapper(jsonMapper: JsonMapper) { this.jsonMapper = javalinLazy { jsonMapper } }
    fun fileRenderer(fileRenderer: FileRenderer) = appData(FileRendererKey, fileRenderer)
    fun <CFG> registerPlugin(plugin: Plugin<CFG>): Plugin<CFG> = plugin.also { pluginManager.register(plugin) }
    fun <T : Any?> appData(key: Key<T>, value: T) = appDataManager.register(key, value)

    companion object {
        @JvmStatic
        fun applyUserConfig(cfg: JavalinState, userConfig: Consumer<JavalinState>) {
            addValidationExceptionMapper(cfg) // add default mapper for validation
            userConfig.accept(cfg) // apply user config to the default config
            cfg.pluginManager.startPlugins()
            cfg.appDataManager.registerIfAbsent(ContextResolverKey, cfg.contextResolver)
            cfg.appDataManager.registerIfAbsent(AsyncExecutorKey, cfg.asyncExecutor.value)
            cfg.appDataManager.registerIfAbsent(ValidationKey, Validation(cfg.validation))
            cfg.appDataManager.registerIfAbsent(FileRendererKey, NotImplementedRenderer())
            cfg.appDataManager.registerIfAbsent(MaxRequestSizeKey, cfg.http.maxRequestSize)
        }
    }
    //@formatter:on

}
