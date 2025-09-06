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

    private fun ConfigurableHandler.getResource(path: String): Pair<Resource?, Boolean>? {
        return try {
            if (baseResource == null) return null
            val resource = baseResource.resolve(path)
            if (resource != null && resource.exists() && !resource.isDirectory) {
                var aliasCheckPassed = false
                // Check for alias - by default, block aliases for security unless explicitly allowed
                if (resource.isAlias) {
                    if (config.aliasCheck != null) {
                        // Apply the configured alias check using correct method name
                        if (!config.aliasCheck!!.checkAlias(path, resource)) {
                            return null // Alias check failed, return null to trigger 404
                        }
                        aliasCheckPassed = true
                    } else {
                        // No alias check configured - default is to block all aliases for security
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

    private fun fileOrWelcomeFile(handler: ConfigurableHandler, target: String): Resource? {
        val (resource, aliasCheckPassed) = handler.getResource(target) ?: (null to false)
        return resource?.fileOrNull(aliasCheckPassed)
            ?: handler.getResource("${target.removeSuffix("/")}/index.html")?.first?.fileOrNull()
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

    private fun serveResourceDirectly(resource: Resource, target: String, ctx: Context, config: StaticFileConfig) {
        val mimeTypes = MimeTypes()

        // Apply custom mime types from configuration
        val customMimeType = config.mimeTypes.getMapping().entries.firstOrNull {
            target.endsWith(".${it.key}", ignoreCase = true)
        }?.value

        val contentType = customMimeType ?: mimeTypes.getMimeByExtension(target)

        // Set content type
        if (contentType != null) {
            ctx.contentType(contentType)
        }

        // Handle ETag
        val weakETag = resource.weakETag
        ctx.header(Header.IF_NONE_MATCH)?.let { requestEtag ->
            if (requestEtag == weakETag) {
                ctx.status(304)
                return
            }
        }
        ctx.header(Header.ETAG, weakETag)

        // Serve the resource content - read all bytes to avoid channel issues
        val bytes = resource.newInputStream().use { it.readAllBytes() }

        // Don't set content-length manually - let Javalin handle it after compression
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
        // TODO: Set alias checks if configured for Jetty 12 - need to investigate correct API
        // if (config.aliasCheck != null) {
        //     // aliasCheckers.add(config.aliasCheck)
        // }
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

private val Resource.weakETag: String get() = EtagUtils.computeWeakEtag(this)
