/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.config

import io.javalin.Javalin
import io.javalin.http.servlet.JavalinServletContext
import io.javalin.http.servlet.TaskInitializer
import io.javalin.http.staticfiles.ResourceHandler
import io.javalin.json.JsonMapper
import io.javalin.plugin.Plugin
import io.javalin.rendering.FileRenderer

/**
 * Public-facing configuration API exposed to users in [Javalin.create].
 * This class provides a safe subset of configuration options, hiding internal APIs.
 *
 * For advanced/dangerous configuration options, use [Javalin.unsafe] to access [JavalinState] directly.
 *
 * @see JavalinState
 */
class JavalinConfig internal constructor(
    /**
     * Advanced/unsafe API providing access to internal Javalin configuration.
     * This exposes powerful but potentially dangerous APIs. Use with caution.
     */
    @JvmField val unsafe: JavalinState
) {

    // CORE CONFIGS - HTTP, routing, and server
    @JvmField val http = unsafe.http
    @JvmField val router = unsafe.router
    @JvmField val jetty = unsafe.jetty

    // FEATURE CONFIGS - Static files, SPAs, and routes
    @JvmField val staticFiles = unsafe.staticFiles
    @JvmField val spaRoot = unsafe.spaRoot
    @JvmField val routes = unsafe.routes

    // CROSS-CUTTING CONFIGS - Validation, context resolution, logging, plugins, events
    @JvmField val validation = unsafe.validation
    @JvmField val contextResolver = unsafe.contextResolver
    @JvmField val requestLogger = unsafe.requestLogger
    @JvmField val bundledPlugins = unsafe.bundledPlugins
    @JvmField val events = unsafe.events

    // MISC SETTINGS - General application-level settings
    @JvmField val startup = unsafe.startup
    @JvmField val concurrency = unsafe.concurrency

    // PUBLIC METHODS - Delegated to unsafe
    fun requestLifeCycle(vararg requestLifecycle: TaskInitializer<JavalinServletContext>) = unsafe.requestLifeCycle(*requestLifecycle)
    fun jsonMapper(jsonMapper: JsonMapper) = unsafe.jsonMapper(jsonMapper)
    fun fileRenderer(fileRenderer: FileRenderer) = unsafe.fileRenderer(fileRenderer)
    fun resourceHandler(resourceHandler: ResourceHandler) = unsafe.resourceHandler(resourceHandler)
    fun <CFG> registerPlugin(plugin: Plugin<CFG>) = unsafe.registerPlugin(plugin)
    fun <T> appData(key: Key<T>, value: T) = unsafe.appData(key, value)
}
