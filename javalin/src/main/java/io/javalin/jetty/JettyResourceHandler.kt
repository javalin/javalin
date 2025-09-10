/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.jetty

import io.javalin.config.PrivateConfig
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.http.staticfiles.Location
import io.javalin.http.staticfiles.StaticFileConfig
import io.javalin.security.RouteRole
import io.javalin.util.JavalinException
import io.javalin.util.JavalinLogger
import jakarta.servlet.http.HttpServletRequest
import org.eclipse.jetty.http.EtagUtils
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.io.EofException
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceFactory
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.absolute
import io.javalin.http.staticfiles.ResourceHandler as JavalinResourceHandler

class JettyResourceHandler(val pvt: PrivateConfig) : JavalinResourceHandler {

    fun init() { // we delay the creation of ConfigurableHandler objects to get our logs in order during startup
        handlers.addAll(lateInitConfigs.map { ConfigurableHandler(it, pvt.jetty.server!!) })
    }

    private val lateInitConfigs = mutableListOf<StaticFileConfig>()
    private val handlers = mutableListOf<ConfigurableHandler>()
    private val mimeTypes = MimeTypes() // Cache MimeTypes instance instead of creating per request

    override fun addStaticFileConfig(config: StaticFileConfig): Boolean =
        if (pvt.jetty.server?.isStarted == true) handlers.add(ConfigurableHandler(config, pvt.jetty.server!!)) else lateInitConfigs.add(config)

    override fun canHandle(ctx: Context) = matchingHandlers(ctx.req(), ctx.target).any { (handler, resourcePath) ->
        try {
            handler.resolveResource(resourcePath) != null
        } catch (e: Exception) {
            false
        }
    }

    override fun handle(ctx: Context): Boolean {
        matchingHandlers(ctx.req(), ctx.target).forEach { (handler, resourcePath) ->
            try {
                // Set custom headers first
                handler.config.headers.forEach { ctx.header(it.key, it.value) }
                
                // Use simplified resource resolution that leverages Jetty's capabilities
                val resource = handler.resolveResource(resourcePath)
                if (resource != null) {
                    return when (handler.config.precompress) {
                        true -> JettyPrecompressingResourceHandler.handle(resourcePath, resource, ctx, pvt.compressionStrategy, handler.config)
                        false -> {
                            serveResourceDirectly(resource, resourcePath, ctx, handler.config)
                            true
                        }
                    }
                }
            } catch (e: EofException) {
                // ignore
            } catch (e: Exception) {
                if (e.message?.contains("Rejected alias reference") == true || e.message?.contains("Failed alias check") == true) return@forEach
                throw e
            }
        }
        return false
    }

    /**
     * Resolve resource with welcome file support and alias checking.
     * Leverages Jetty's native capabilities where possible.
     */
    private fun ConfigurableHandler.resolveResource(path: String): Resource? = try {
        baseResource?.resolve(path)?.takeIf { it.exists() }?.let { resource ->
            if (resource.isDirectory) {
                // Try welcome files using Jetty's configured welcome files
                welcomeFiles.asSequence()
                    .mapNotNull { resource.resolve(it)?.takeIf { it.exists() && !it.isDirectory } }
                    .firstOrNull()
                    ?.let { checkAliasIfNeeded(it, "$path/${it.getFileName()}") }
            } else if (!resource.uri.schemeSpecificPart.endsWith('/')) {
                checkAliasIfNeeded(resource, path)
            } else null
        }
    } catch (e: Exception) { null }

    private fun ConfigurableHandler.checkAliasIfNeeded(resource: Resource, path: String): Resource? =
        if (resource.isAlias && config.aliasCheck?.checkAlias(path, resource) != true) null else resource

    private fun matchingHandlers(request: HttpServletRequest, target: String): Sequence<Pair<ConfigurableHandler, String>> =
        handlers.asSequence()
            .filter { !it.config.skipFileFunction(request) }
            .mapNotNull { handler ->
                val hostedPath = handler.config.hostedPath
                when {
                    hostedPath == "/" -> handler to target
                    target.startsWith(hostedPath) -> handler to target.removePrefix(hostedPath).removePrefix("/")
                    else -> null
                }
            }

    private val Context.target get() = this.req().requestURI.removePrefix(this.req().contextPath)

    private fun serveResourceDirectly(resource: Resource, target: String, ctx: Context, config: StaticFileConfig) {
        // Determine content type (custom types override default)
        val contentType = config.mimeTypes.getMapping().entries
            .firstOrNull { target.endsWith(".${it.key}", ignoreCase = true) }?.value
            ?: mimeTypes.getMimeByExtension(target)

        contentType?.let { ctx.contentType(it) }

        // Handle ETag for conditional requests
        val weakETag = resource.weakETag
        if (ctx.header(Header.IF_NONE_MATCH) == weakETag) {
            ctx.status(304)
            return
        }
        ctx.header(Header.ETAG, weakETag)

        // Serve content efficiently
        ctx.result(resource.newInputStream().use { it.readAllBytes() })
    }

    override fun getResourceRouteRoles(ctx: Context): Set<RouteRole> =
        matchingHandlers(ctx.req(), ctx.target)
            .firstOrNull { (handler, resourcePath) -> handler.resolveResource(resourcePath) != null }
            ?.first?.config?.roles ?: emptySet()

}

open class ConfigurableHandler(val config: StaticFileConfig, jettyServer: Server) : ResourceHandler() {

    init {
        JavalinLogger.info("Static file handler added: ${config.refinedToString()}. File system location: '${getResourceBase(config)}'")
        baseResource = getResourceBase(config)
        isDirAllowed = false
        isEtags = true
        welcomeFiles = listOf("index.html") // Use Jetty's native welcome file capability
        server = jettyServer
        start()
    }

    private fun getResourceBase(config: StaticFileConfig): Resource {
        return when (config.location) {
            Location.CLASSPATH -> {
                val errorMsg = "Static resource directory with path: '${config.directory}' does not exist."
                ResourceFactory.of(this).newClassLoaderResource(config.directory, false)
                    ?: throw JavalinException("$errorMsg Depending on your setup, empty folders might not get copied to classpath.")
            }
            else -> {
                val absolutePath = Path(config.directory).absolute().normalize()
                val errorMsg = "Static resource directory with path: '$absolutePath' does not exist."
                if (!Files.exists(absolutePath)) throw JavalinException(errorMsg)
                ResourceFactory.of(this).newResource(config.directory)
            }
        }
    }

}

private val Resource.weakETag: String get() = EtagUtils.computeWeakEtag(this)
