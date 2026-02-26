/*
 * Javalin - https://javalin.io
 * Copyright 2020 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.micrometer

import io.javalin.config.JavalinState
import io.javalin.http.Context
import io.javalin.http.ExceptionHandler
import io.javalin.http.HandlerType
import io.javalin.http.HttpStatus
import io.javalin.plugin.Plugin
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.binder.jetty.JettyConnectionMetrics
import io.micrometer.core.instrument.binder.jetty.JettyServerThreadPoolMetrics
import org.slf4j.LoggerFactory
import java.util.function.Consumer
import kotlin.streams.asSequence

/**
 * Micrometer plugin for Javalin that provides HTTP request metrics and Jetty server metrics.
 *
 * Example usage:
 * ```kotlin
 * Javalin.create { config ->
 *     config.registerPlugin(MicrometerPlugin {
 *         it.registry = SimpleMeterRegistry()
 *         it.tags = Tags.of("service", "my-app")
 *     })
 * }
 * ```
 */
class MicrometerPlugin @JvmOverloads constructor(
    userConfig: Consumer<Config>? = null
) : Plugin<MicrometerPlugin.Config>(userConfig, Config()) {

    class Config {
        /** The meter registry to use. Defaults to the global registry. */
        @JvmField
        var registry: MeterRegistry = Metrics.globalRegistry

        /** Additional tags to add to all metrics */
        @JvmField
        var tags: Iterable<Tag> = Tags.empty()

        /** Whether to tag exception names in metrics */
        @JvmField
        var tagExceptionName: Boolean = false

        /** Whether to tag redirect paths (3xx responses) with actual path instead of "REDIRECTION" */
        @JvmField
        var tagRedirectPaths: Boolean = false

        /** Whether to tag 404 responses from mapped paths with actual path instead of "NOT_FOUND" */
        @JvmField
        var tagNotFoundMappedPaths: Boolean = false
    }

    override fun onInitialize(state: JavalinState) {
        // Register for request completed events to collect metrics
        state.events.requestCompleted { ctx, executionTimeMs ->
            recordHttpMetrics(ctx, executionTimeMs, state)
        }

        // Register exception handler if exception tagging is enabled
        if (pluginConfig.tagExceptionName) {
            // Store the exception handler for user to delegate to
            state.routes.exception(Exception::class.java, exceptionHandler)
        }

        logger.info("MicrometerPlugin initialized with registry: {}", pluginConfig.registry.javaClass.simpleName)
    }

    override fun onStart(state: JavalinState) {
        // Bind Jetty server metrics
        state.jettyInternal.server?.server?.let { server ->
            // Thread pool metrics
            JettyServerThreadPoolMetrics(server.threadPool, pluginConfig.tags).bindTo(pluginConfig.registry)

            // Connection metrics - need to add after server starts
            JettyConnectionMetrics.addToAllConnectors(server, pluginConfig.registry, pluginConfig.tags)

            logger.info("Jetty server metrics bound to registry")
        }
    }

    private fun recordHttpMetrics(ctx: Context, executionTimeMs: Float, state: JavalinState) {
        try {
            val method = ctx.method().toString()
            val status = ctx.res().status.toString()
            val uri = determineUri(ctx, state)
            val exceptionName = if (pluginConfig.tagExceptionName) {
                ctx.res().getHeader(EXCEPTION_HEADER) ?: "None"
            } else {
                "None"
            }

            // Clear the exception header after reading it
            if (pluginConfig.tagExceptionName) {
                ctx.res().setHeader(EXCEPTION_HEADER, null)
            }

            val outcome = determineOutcome(ctx.res().status, exceptionName)

            // Build tags
            val metricTags = Tags.concat(
                pluginConfig.tags,
                "method", method,
                "uri", uri,
                "status", status,
                "outcome", outcome,
                "exception", exceptionName
            )

            // Record timer for request duration
            Timer.builder("http.server.requests")
                .description("HTTP server request metrics")
                .tags(metricTags)
                .register(pluginConfig.registry)
                .record(executionTimeMs.toLong(), java.util.concurrent.TimeUnit.MILLISECONDS)

        } catch (e: Exception) {
            logger.warn("Failed to record HTTP metrics", e)
        }
    }

    private fun determineUri(ctx: Context, state: JavalinState): String {
        val statusCode = ctx.res().status
        val pathInfo = ctx.path().removePrefix(state.router.contextPath).prefixIfNot("/")

        // Check if this is a redirect
        if (!pluginConfig.tagRedirectPaths && statusCode in 300..399) {
            return "REDIRECTION"
        }

        // Try to find matched path
        val handlerType = HandlerType.findOrCreate(ctx.method().toString())
        if (handlerType != null) {
            val matchedPath = state.internalRouter.findHttpHandlerEntries(handlerType, pathInfo)
                .asSequence()
                .map { it.endpoint.path }
                .map { if (it == "/" || it.isBlank()) "root" else it }
                .firstOrNull()

            if (matchedPath != null) {
                return matchedPath
            }
        }

        // If 404 and we're not tagging mapped paths, return NOT_FOUND
        if (!pluginConfig.tagNotFoundMappedPaths && statusCode == 404) {
            return "NOT_FOUND"
        }

        // Check if there's a matched path from the context
        val ctxMatchedPath = if (ctx.endpoints().list().isNotEmpty()) ctx.endpoint().path else ""
        if (ctxMatchedPath.isNotBlank()) {
            return if (ctxMatchedPath == "/") "root" else ctxMatchedPath
        }

        // Default to NOT_FOUND for unmapped paths
        return "NOT_FOUND"
    }

    private fun determineOutcome(statusCode: Int, exceptionName: String): String {
        return when {
            exceptionName != "None" -> "SERVER_ERROR"
            statusCode in 200..299 -> "SUCCESS"
            statusCode in 300..399 -> "REDIRECTION"
            statusCode in 400..499 -> "CLIENT_ERROR"
            statusCode >= 500 -> "SERVER_ERROR"
            else -> "UNKNOWN"
        }
    }

    private fun String.prefixIfNot(prefix: String): String {
        return if (this.startsWith(prefix)) this else "$prefix$this"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MicrometerPlugin::class.java)
        private const val EXCEPTION_HEADER = "__micrometer_exception_name"

        /**
         * Exception handler that can be used to tag exceptions in metrics.
         * Users should delegate to this handler in their own exception handling code.
         */
        @JvmField
        val exceptionHandler = ExceptionHandler<Exception> { e, ctx ->
            val simpleName = e.javaClass.simpleName
            ctx.header(EXCEPTION_HEADER, simpleName.ifBlank { e.javaClass.name })
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}
