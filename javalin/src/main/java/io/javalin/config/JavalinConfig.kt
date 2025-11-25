/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.config

import io.javalin.Javalin
import io.javalin.http.servlet.JavalinServletContext
import io.javalin.http.servlet.TaskInitializer
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

    // PUBLIC CONFIG SECTIONS - References delegated to JavalinState
    @JvmField val misc = state.misc
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
    @JvmField val validation = state.validation

    // PUBLIC METHODS - Delegated to state
    fun requestLifeCycle(vararg requestLifecycle: TaskInitializer<JavalinServletContext>) = state.requestLifeCycle(*requestLifecycle)
    fun events(listener: Consumer<EventConfig>) = state.events(listener)
    fun jsonMapper(jsonMapper: JsonMapper) = state.jsonMapper(jsonMapper)
    fun fileRenderer(fileRenderer: FileRenderer) = state.fileRenderer(fileRenderer)
    fun <CFG> registerPlugin(plugin: Plugin<CFG>) = state.registerPlugin(plugin)
    fun <T : Any?> appData(key: Key<T>, value: T) = state.appData(key, value)
}
