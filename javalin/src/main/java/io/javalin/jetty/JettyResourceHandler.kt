/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.jetty

import io.javalin.config.PrivateConfig
import io.javalin.http.Context
import io.javalin.http.staticfiles.Location
import io.javalin.http.staticfiles.StaticFileConfig
import io.javalin.security.RouteRole
import io.javalin.util.JavalinException
import io.javalin.util.JavalinLogger
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

    override fun addStaticFileConfig(config: StaticFileConfig): Boolean =
        if (pvt.jetty.server?.isStarted == true) handlers.add(ConfigurableHandler(config, pvt.jetty.server!!)) else lateInitConfigs.add(config)

    override fun canHandle(ctx: Context) = findHandler(ctx) != null

    override fun handle(ctx: Context): Boolean {
        val (handler, resourcePath) = findHandler(ctx) ?: return false

        try {
            // Apply custom headers
            handler.config.headers.forEach { ctx.header(it.key, it.value) }

            return if (handler.config.precompress) {
                val resource = handler.getResource(resourcePath) ?: return false
                JettyPrecompressingResourceHandler.handle(resourcePath, resource, ctx, pvt.compressionStrategy, handler.config)
            } else {
                // Use Jetty's native resource resolution and serving capabilities
                handler.handleResource(resourcePath, ctx)
            }
        } catch (e: EofException) {
            return false
        } catch (e: Exception) {
            if (e.message?.contains("alias") == true) return false
            throw e
        }
    }

    private fun findHandler(ctx: Context): Pair<ConfigurableHandler, String>? {
        val target = ctx.req().requestURI.removePrefix(ctx.req().contextPath)
        return handlers.asSequence()
            .filter { !it.config.skipFileFunction(ctx.req()) }
            .mapNotNull { handler ->
                val hostedPath = handler.config.hostedPath
                when {
                    hostedPath == "/" -> handler to target
                    target.startsWith(hostedPath) -> {
                        val resourcePath = target.removePrefix(hostedPath).removePrefix("/")
                        handler to resourcePath
                    }
                    else -> null
                }
            }
            .firstOrNull { (handler, resourcePath) ->
                handler.getResource(resourcePath) != null
            }
    }

    override fun getResourceRouteRoles(ctx: Context): Set<RouteRole> =
        findHandler(ctx)?.first?.config?.roles ?: emptySet()

}

open class ConfigurableHandler(val config: StaticFileConfig, jettyServer: Server) : ResourceHandler() {

    private val jettyMimeTypes = MimeTypes() // Use Jetty's native MIME type handling

    init {
        JavalinLogger.info("Static file handler added: ${config.refinedToString()}. File system location: '${getResourceBase(config)}'")
        baseResource = getResourceBase(config)
        isDirAllowed = false
        isEtags = true
        welcomeFiles = listOf("index.html") // Use Jetty's native welcome file capability
        server = jettyServer
        start()
    }

    fun getResource(path: String): Resource? {
        return try {
            if (baseResource == null) return null

            // Try to resolve the direct resource first
            baseResource.resolve(path)?.let { resource ->
                if (resource.exists() && !resource.isDirectory && isValidResource(resource, path)) {
                    return resource
                }
            }

            // Check for welcome file (index.html) using Jetty's native logic
            val welcomePath = "${path.removeSuffix("/")}/index.html"
            baseResource.resolve(welcomePath)?.let { welcomeResource ->
                if (welcomeResource.exists() && !welcomeResource.isDirectory && isValidResource(welcomeResource, welcomePath)) {
                    return welcomeResource
                }
            }

            null
        } catch (e: Exception) {
            null
        }
    }

    private fun isValidResource(resource: Resource, path: String): Boolean {
        if (!resource.isAlias) return true

        // No alias check configured - default is to block all aliases for security
        if (config.aliasCheck == null) return false

        // Apply the configured alias check
        return config.aliasCheck!!.checkAlias(path, resource)
    }

    fun handleResource(resourcePath: String, ctx: Context): Boolean {
        val resource = getResource(resourcePath) ?: return false

        // Use Jetty's native content type resolution with custom override support
        val contentType = config.mimeTypes.getMapping().entries
            .firstOrNull { resourcePath.endsWith(".${it.key}", ignoreCase = true) }?.value
            ?: jettyMimeTypes.getMimeByExtension(resourcePath)

        contentType?.let { ctx.contentType(it) }

        // Use Jetty's native ETag support
        if (isEtags) {
            val etag = resource.etagValue
            if (etag != null) {
                // Handle conditional requests using Jetty's ETag logic
                if (ctx.header("If-None-Match") == etag) {
                    ctx.status(304)
                    return true
                }
                ctx.header("ETag", etag)
            }
        }

        // Serve the resource content
        ctx.result(resource.newInputStream().use { it.readAllBytes() })
        return true
    }

    private val Resource.etagValue: String?
        get() = try {
            org.eclipse.jetty.http.EtagUtils.computeWeakEtag(this)
        } catch (e: Exception) {
            null
        }

    private fun getResourceBase(config: StaticFileConfig): Resource {
        val noSuchDirMessageBuilder: (String) -> String = { "Static resource directory with path: '$it' does not exist." }
        val classpathHint = "Depending on your setup, empty folders might not get copied to classpath."
        if (config.location == Location.CLASSPATH) {
            return ResourceFactory.of(this)
                .newClassLoaderResource(config.directory, false)
                ?: throw JavalinException("${noSuchDirMessageBuilder(config.directory)} $classpathHint")
        }

        // Use the absolute path as this aids in debugging. Issues frequently come from incorrect root directories, not incorrect relative paths.
        val absoluteDirectoryPath = Path(config.directory).absolute().normalize()
        if (!Files.exists(absoluteDirectoryPath)) {
            throw JavalinException(noSuchDirMessageBuilder(absoluteDirectoryPath.toString()))
        }
        return ResourceFactory.of(this).newResource(config.directory)
    }

}
