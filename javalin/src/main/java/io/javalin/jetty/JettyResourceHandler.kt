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

/**
 * Javalin's static file handler implementation leveraging Jetty 12 native capabilities.
 * 
 * This handler serves static files by combining Jetty 12's built-in ResourceHandler
 * capabilities with custom logic for Javalin-specific features like custom headers,
 * compression strategies, and security controls.
 * 
 * Key native Jetty 12 features leveraged:
 * - ResourceHandler.welcomeFiles for index.html handling
 * - EtagUtils.computeWeakEtag() for consistent ETag computation
 * - MimeTypes for efficient MIME type resolution
 * - Resource.resolve() for path resolution
 * - Built-in alias checking foundation
 * 
 * Custom features added on top:
 * - StaticFileConfig support (custom headers, roles, skip functions)
 * - Pre-compression with caching
 * - Custom MIME type mappings
 * - Security-focused alias checking
 * - Integration with Javalin's compression strategy
 */
class JettyResourceHandler(val pvt: PrivateConfig) : JavalinResourceHandler {

    fun init() { // we delay the creation of ConfigurableHandler objects to get our logs in order during startup
        handlers.addAll(lateInitConfigs.map { ConfigurableHandler(it, pvt.jetty.server!!) })
    }

    private val lateInitConfigs = mutableListOf<StaticFileConfig>()
    private val handlers = mutableListOf<ConfigurableHandler>()
    
    // Reuse single MimeTypes instance for better performance
    private val mimeTypes = MimeTypes()

    override fun addStaticFileConfig(config: StaticFileConfig): Boolean =
        if (pvt.jetty.server?.isStarted == true) handlers.add(ConfigurableHandler(config, pvt.jetty.server!!)) else lateInitConfigs.add(config)

    override fun canHandle(ctx: Context) = matchingHandlers(ctx.req(), ctx.target).any { (handler, resourcePath) ->
        try {
            fileOrWelcomeFile(handler, resourcePath) != null
        } catch (e: Exception) {
            false
        }
    }

    override fun handle(ctx: Context): Boolean {
        matchingHandlers(ctx.req(), ctx.target).forEach { (handler, resourcePath) ->
            try {
                val fileOrWelcomeFile = fileOrWelcomeFile(handler, resourcePath)
                if (fileOrWelcomeFile != null) {
                    handler.config.headers.forEach { ctx.header(it.key, it.value) } // set user headers
                    return when (handler.config.precompress) {
                        true -> JettyPrecompressingResourceHandler.handle(resourcePath, fileOrWelcomeFile, ctx, pvt.compressionStrategy, handler.config)
                        false -> {
                            try {
                                // Handle resource manually without precompression
                                serveResourceDirectly(fileOrWelcomeFile, resourcePath, ctx, handler.config)
                                true
                            } catch (e: Exception) {
                                false
                            }
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
     * It looks like jetty resolves the file even if the path contains `/` in the end.
     * [Resource.isDirectory] returns `false` in this case, and we need to explicitly check
     * if [Resource.getURI] ends with `/`
     * However, when alias checking is enabled and passes, we should allow such resources.
     */
    private fun Resource?.fileOrNull(aliasCheckPassed: Boolean = false): Resource? =
        this?.takeIf {
            it.exists() && !it.isDirectory &&
            (!it.uri.schemeSpecificPart.endsWith('/') || aliasCheckPassed)
        }

    /**
     * Enhanced resource resolution leveraging Jetty 12 native ResourceHandler capabilities.
     * This method attempts to use the native ResourceHandler's resource resolution first,
     * falling back to custom logic only when necessary for security (alias checking).
     */
    private fun ConfigurableHandler.getResourceWithNativeSupport(path: String): Pair<Resource?, Boolean>? {
        return try {
            if (baseResource == null) return null
            
            // Use the native ResourceHandler's resource resolution first
            val resource = baseResource.resolve(path)
            if (resource != null && resource.exists() && !resource.isDirectory) {
                var aliasCheckPassed = false
                
                // Security-critical alias checking - maintain custom logic for safety
                if (resource.isAlias) {
                    if (config.aliasCheck != null) {
                        if (!config.aliasCheck!!.checkAlias(path, resource)) {
                            return null // Alias check failed
                        }
                        aliasCheckPassed = true
                    } else {
                        // Default security behavior: block all aliases
                        return null
                    }
                }
                Pair(resource, aliasCheckPassed)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Welcome file resolution leveraging Jetty 12 ResourceHandler configuration.
     * This method attempts to find the requested resource, falling back to welcome files
     * as configured in the native ResourceHandler (index.html).
     */
    private fun fileOrWelcomeFile(handler: ConfigurableHandler, target: String): Resource? {
        // First, try to get the directly requested resource
        val (resource, aliasCheckPassed) = handler.getResourceWithNativeSupport(target) ?: (null to false)
        val directResource = resource?.fileOrNull(aliasCheckPassed)
        
        if (directResource != null) {
            return directResource
        }
        
        // Fall back to welcome file (index.html) - leveraging the configuration we set
        // in ConfigurableHandler.welcomeFiles but handling it manually due to our custom routing
        val welcomeResource = handler.getResourceWithNativeSupport("${target.removeSuffix("/")}/index.html")?.first
        return welcomeResource?.fileOrNull()
    }

    private fun nonSkippedHandlers(request: HttpServletRequest) =
        handlers.asSequence().filter { !it.config.skipFileFunction(request) }

    private fun matchingHandlers(request: HttpServletRequest, target: String): Sequence<Pair<ConfigurableHandler, String>> =
        nonSkippedHandlers(request).mapNotNull { handler ->
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

    private val Context.target get() = this.req().requestURI.removePrefix(this.req().contextPath)

    /**
     * Serve a resource directly using optimized Jetty 12 capabilities.
     * This method leverages native Jetty features for MIME type resolution and ETag handling
     * while supporting custom configuration options.
     */
    private fun serveResourceDirectly(resource: Resource, target: String, ctx: Context, config: StaticFileConfig) {
        // Apply custom mime types from configuration first, then fall back to Jetty's standard resolution
        val customMimeType = config.mimeTypes.getMapping().entries.firstOrNull {
            target.endsWith(".${it.key}", ignoreCase = true)
        }?.value

        val contentType = customMimeType ?: mimeTypes.getMimeByExtension(target)

        // Set content type using Jetty's MIME resolution
        if (contentType != null) {
            ctx.contentType(contentType)
        }

        // Handle ETag using Jetty's built-in weak ETag computation
        // This leverages the same ETag logic used throughout Jetty
        val weakETag = resource.weakETag
        ctx.header(Header.IF_NONE_MATCH)?.let { requestEtag ->
            if (requestEtag == weakETag) {
                ctx.status(304)
                return
            }
        }
        ctx.header(Header.ETAG, weakETag)

        // Efficiently serve the resource content
        // Reading all bytes maintains compatibility with compression and other filters
        val bytes = resource.newInputStream().use { it.readAllBytes() }
        ctx.result(bytes)
    }

    override fun getResourceRouteRoles(ctx: Context): Set<RouteRole> {
        matchingHandlers(ctx.req(), ctx.target).forEach { (handler, resourcePath) ->
            val fileOrWelcomeFile = fileOrWelcomeFile(handler, resourcePath)
            if (fileOrWelcomeFile != null) {
                return handler.config.roles;
            }
        }
        return emptySet();
    }

}

open class ConfigurableHandler(val config: StaticFileConfig, jettyServer: Server) : ResourceHandler() {

    init {
        JavalinLogger.info("Static file handler added: ${config.refinedToString()}. File system location: '${getResourceBase(config)}'")
        baseResource = getResourceBase(config)
        isDirAllowed = false
        isEtags = true
        
        // Configure welcome files using Jetty 12 native capability
        // This enables the ResourceHandler to automatically serve index.html for directory requests
        welcomeFiles = listOf("index.html")
        
        // Configure alias checks if provided
        // Alias checking is security-critical to prevent access to files outside the intended directory
        if (config.aliasCheck != null) {
            JavalinLogger.info("Alias check configured for static files: ${config.aliasCheck}")
        }
        
        server = jettyServer
        start()
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

/**
 * Extension property that leverages Jetty 12's native ETag computation.
 * This uses the same weak ETag algorithm that Jetty's ResourceHandler uses internally,
 * ensuring consistency across all static file serving in the application.
 */
private val Resource.weakETag: String get() = EtagUtils.computeWeakEtag(this)
