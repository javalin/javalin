/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */
package io.javalin.core

import io.javalin.Javalin
import io.javalin.core.JettyUtil.disableJettyLogger
import io.javalin.core.compression.Brotli
import io.javalin.core.compression.CompressionStrategy
import io.javalin.core.compression.Gzip
import io.javalin.core.event.EventListener
import io.javalin.core.event.HandlerMetaInfo
import io.javalin.core.event.WsHandlerMetaInfo
import io.javalin.core.plugin.Plugin
import io.javalin.core.plugin.PluginAlreadyRegisteredException
import io.javalin.core.plugin.PluginInitLifecycleViolationException
import io.javalin.core.plugin.PluginLifecycleInit
import io.javalin.core.plugin.PluginNotFoundException
import io.javalin.core.security.AccessManager
import io.javalin.core.security.SecurityUtil
import io.javalin.core.util.CorsPlugin.Companion.forAllOrigins
import io.javalin.core.util.CorsPlugin.Companion.forOrigins
import io.javalin.core.util.LogUtil
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.RequestLogger
import io.javalin.http.SinglePageHandler
import io.javalin.http.staticfiles.JettyResourceHandler
import io.javalin.http.staticfiles.Location
import io.javalin.http.staticfiles.ResourceHandler
import io.javalin.http.staticfiles.StaticFileConfig
import io.javalin.websocket.WsConfig
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ContextHandler.AliasCheck
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import java.util.function.Supplier
import java.util.stream.Stream

/**
 * @JvmField will be fixed once Javalin.java is Javalin.kt
 */

class JavalinConfig {
    @JvmField var autogenerateEtags = false
    @JvmField var prefer405over404 = false
    @JvmField var enforceSsl = false
    @JvmField var precompressStaticFiles = false
    @JvmField var aliasCheckForStaticFiles: AliasCheck? = null
    @JvmField var showJavalinBanner = true
    @JvmField var logIfServerNotStarted = true
    @JvmField var ignoreTrailingSlashes = true
    @JvmField var defaultContentType = "text/plain"
    @JvmField var contextPath = "/"
    @JvmField var maxRequestSize = 1000000L // server will not accept payloads larger than 1mb by default
    @JvmField var asyncRequestTimeout = 0L
    @JvmField var inner: Inner = Inner()

    inner class Inner {
        @JvmField var plugins: MutableMap<Class<out Plugin>, Plugin> = mutableMapOf()
        @JvmField var appAttributes: MutableMap<Class<*>, Any> = mutableMapOf()
        @JvmField var requestLogger: RequestLogger? = null
        @JvmField var resourceHandler: ResourceHandler? = null
        @JvmField var accessManager: AccessManager = AccessManager { handler, ctx, roles -> SecurityUtil.noopAccessManager(handler, ctx, roles) }
        @JvmField var singlePageHandler = SinglePageHandler()
        @JvmField var sessionHandler: SessionHandler? = null
        @JvmField var wsFactoryConfig: Consumer<WebSocketServletFactory>? = null
        @JvmField var wsLogger: WsConfig? = null
        @JvmField var server: Server? = null
        @JvmField var servletContextHandlerConsumer: Consumer<ServletContextHandler>? = null
        @JvmField var compressionStrategy = CompressionStrategy.GZIP
    }

    fun registerPlugin(plugin: Plugin): JavalinConfig {
        if (inner.plugins.containsKey(plugin.javaClass)) {
            throw PluginAlreadyRegisteredException(plugin.javaClass)
        }
        inner.plugins[plugin.javaClass] = plugin
        return this
    }

    /**
     * Get a registered plugin
     */
    fun <T : Plugin> getPlugin(pluginClass: Class<T>): T {
        val plugin = inner.plugins[pluginClass] ?: throw PluginNotFoundException(pluginClass);
        return plugin as T;
    }

    fun enableDevLogging(): JavalinConfig {
        requestLogger(LogUtil::requestDevLogger);
        wsLogger(LogUtil::wsDevLogger);
        return this
    }

    fun enableWebjars(): JavalinConfig {
        return addStaticFiles("/webjars", Location.CLASSPATH)
    }

    fun addStaticFiles(classpathPath: String): JavalinConfig {
        return addStaticFiles(classpathPath, Location.CLASSPATH)
    }

    fun addStaticFiles(path: String, location: Location): JavalinConfig {
        return addStaticFiles("/", path, location)
    }

    fun addStaticFiles(urlPathPrefix: String, path: String, location: Location): JavalinConfig {
        disableJettyLogger()
        if (inner.resourceHandler == null) inner.resourceHandler = JettyResourceHandler(precompressStaticFiles, aliasCheckForStaticFiles)
        inner.resourceHandler!!.addStaticFileConfig(StaticFileConfig(urlPathPrefix, path, location))
        return this
    }

    fun addSinglePageRoot(path: String, filePath: String): JavalinConfig {
        addSinglePageRoot(path, filePath, Location.CLASSPATH)
        return this
    }

    fun addSinglePageRoot(path: String, filePath: String, location: Location): JavalinConfig {
        inner.singlePageHandler.add(path, filePath, location)
        return this
    }

    fun addSinglePageHandler(path: String, customHandler: Handler): JavalinConfig {
        inner.singlePageHandler.add(path, customHandler)
        return this
    }

    fun enableCorsForAllOrigins(): JavalinConfig {
        registerPlugin(forAllOrigins())
        return this
    }

    fun enableCorsForOrigin(vararg origins: String): JavalinConfig {
        registerPlugin(forOrigins(*origins))
        return this
    }

    fun accessManager(accessManager: AccessManager): JavalinConfig {
        inner.accessManager = accessManager
        return this
    }

    fun requestLogger(requestLogger: RequestLogger): JavalinConfig {
        inner.requestLogger = requestLogger
        return this
    }

    fun sessionHandler(sessionHandlerSupplier: Supplier<SessionHandler?>): JavalinConfig {
        disableJettyLogger()
        inner.sessionHandler = sessionHandlerSupplier.get()
        return this
    }

    fun wsFactoryConfig(wsFactoryConfig: Consumer<WebSocketServletFactory>): JavalinConfig {
        inner.wsFactoryConfig = wsFactoryConfig
        return this
    }

    fun wsLogger(ws: Consumer<WsConfig>): JavalinConfig {
        val logger = WsConfig()
        ws.accept(logger)
        inner.wsLogger = logger
        return this
    }

    fun server(server: Supplier<Server?>): JavalinConfig {
        inner.server = server.get()
        return this
    }

    fun configureServletContextHandler(consumer: Consumer<ServletContextHandler>?): JavalinConfig {
        inner.servletContextHandlerConsumer = consumer
        return this
    }

    fun compressionStrategy(brotli: Brotli?, gzip: Gzip?): JavalinConfig {
        inner.compressionStrategy = CompressionStrategy(brotli, gzip)
        return this
    }

    fun compressionStrategy(compressionStrategy: CompressionStrategy): JavalinConfig {
        inner.compressionStrategy = compressionStrategy
        return this
    }

    private fun <T> getPluginsExtending(clazz: Class<T>): Stream<out T> {
        return inner.plugins.values
                .stream()
                .filter { obj: Plugin? -> clazz.isInstance(obj) }
                .map { plugin: Plugin -> plugin as T }
    }

    companion object {
        fun applyUserConfig(app: Javalin, config: JavalinConfig, userConfig: Consumer<JavalinConfig?>) {
            userConfig.accept(config) // apply user config to the default config
            val anyHandlerAdded = AtomicBoolean(false)
            app.events { listener: EventListener ->
                listener.handlerAdded { x: HandlerMetaInfo? -> anyHandlerAdded.set(true) }
                listener.wsHandlerAdded { x: WsHandlerMetaInfo? -> anyHandlerAdded.set(true) }
            }
            config.getPluginsExtending(PluginLifecycleInit::class.java)
                    .forEach { plugin: PluginLifecycleInit ->
                        plugin.init(app)
                        if (anyHandlerAdded.get()) { // check if any "init" added a handler
                            throw PluginInitLifecycleViolationException((plugin as Plugin).javaClass)
                        }
                    }
            config.inner.plugins.values.forEach(Consumer { plugin: Plugin -> plugin.apply(app) })
            if (config.enforceSsl) {
                app.before(SecurityUtil::sslRedirect)
            }
        }
    }
}
