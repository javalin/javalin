/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.config

import io.javalin.Javalin
import io.javalin.json.JsonMapper
import io.javalin.plugin.Plugin
import io.javalin.rendering.FileRenderer
import java.util.function.Consumer

/**
 * Public-facing configuration API exposed to users in [Javalin.create].
 * This class provides a safe subset of configuration options, hiding internal APIs.
 * 
 * For advanced/dangerous configuration options, use [Javalin.unsafe] to access [JavalinState] directly.
 * 
 * @see JavalinState
 */
class JavalinConfig internal constructor(internal val state: JavalinState) {
    
    // PUBLIC CONFIG SECTIONS - Immutable references delegated to JavalinState
    @JvmField val http = state.http
    @JvmField val router = state.router
    @JvmField val contextResolver = state.contextResolver
    @JvmField val routes = state.routes
    @JvmField val jetty = state.jetty
    @JvmField val staticFiles = state.staticFiles
    @JvmField val spaRoot = state.spaRoot
    @JvmField val requestLogger = state.requestLogger
    @JvmField val bundledPlugins = state.bundledPlugins
    @JvmField val events = state.events
    
    // MUTABLE PUBLIC FIELDS - Direct fields initialized by copying from state
    @JvmField var validation: ValidationConfig = state.validation
    @JvmField var useVirtualThreads: Boolean = state.useVirtualThreads
    @JvmField var showJavalinBanner: Boolean = state.showJavalinBanner
    @JvmField var showOldJavalinVersionWarning: Boolean = state.showOldJavalinVersionWarning
    @JvmField var startupWatcherEnabled: Boolean = state.startupWatcherEnabled
    @JvmField var servletRequestLifecycle: MutableList<io.javalin.http.servlet.TaskInitializer<io.javalin.http.servlet.JavalinServletContext>> = state.servletRequestLifecycle
    
    // PUBLIC METHODS - Delegated to state
    fun events(listener: Consumer<EventConfig>) = state.events(listener)
    fun jsonMapper(jsonMapper: JsonMapper) = state.jsonMapper(jsonMapper)
    fun fileRenderer(fileRenderer: FileRenderer) = state.fileRenderer(fileRenderer)
    fun <CFG> registerPlugin(plugin: Plugin<CFG>) = state.registerPlugin(plugin)
    fun <T : Any?> appData(key: Key<T>, value: T) = state.appData(key, value)
}
