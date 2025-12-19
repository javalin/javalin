/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.jetty

import io.javalin.config.JavalinState
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.http.staticfiles.Location
import io.javalin.http.staticfiles.StaticFileConfig
import io.javalin.security.RouteRole
import io.javalin.util.JavalinException
import io.javalin.util.JavalinLogger
import org.eclipse.jetty.http.EtagUtils.computeWeakEtag
import org.eclipse.jetty.io.EofException
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.ResourceHandler
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceFactory
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.absolute
import io.javalin.http.staticfiles.ResourceHandler as JavalinResourceHandler

class JettyResourceHandler(val cfg: JavalinState) : JavalinResourceHandler {

    fun init() { // we delay the creation of ConfigurableHandler objects to get our logs in order during startup
        handlers.addAll(lateInitConfigs.map { ConfigurableHandler(it, cfg.jettyInternal.server!!) })
    }

    private val lateInitConfigs = mutableListOf<StaticFileConfig>()
    private val handlers = mutableListOf<ConfigurableHandler>()
    internal val precompressingHandler = JettyPrecompressingResourceHandler()

    override fun addStaticFileConfig(config: StaticFileConfig): Boolean =
        if (cfg.jettyInternal.server?.isStarted == true) handlers.add(ConfigurableHandler(config, cfg.jettyInternal.server!!)) else lateInitConfigs.add(config)

    override fun canHandle(ctx: Context) = findHandler(ctx) != null

    override fun handle(ctx: Context): Boolean {
        val (handler, resourcePath) = findHandler(ctx) ?: return false
        try {
            handler.config.headers.forEach { ctx.header(it.key, it.value) }
            return if (handler.config.precompressMaxSize > 0) {
                precompressingHandler.handle(resourcePath, ctx, cfg.http.compressionStrategy, handler)
            } else {
                handler.handleResource(resourcePath, ctx)
            }
        } catch (_: EofException) {
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
                if (hostedPath != "/" && !target.startsWith(hostedPath)) return@mapNotNull null
                handler to (if (hostedPath == "/") target else target.removePrefix(hostedPath).removePrefix("/"))
            }
            .find { (handler, resourcePath) -> handler.getResource(resourcePath) != null }
    }

    override fun resourceRouteRoles(ctx: Context): Set<RouteRole> =
        findHandler(ctx)?.first?.config?.roles ?: emptySet()

}

open class ConfigurableHandler(val config: StaticFileConfig, jettyServer: Server) : ResourceHandler() {

    init {
        JavalinLogger.info("Static file handler added: ${config.refinedToString()}. File system location: '${getResourceBase(config)}'")
        baseResource = getResourceBase(config)
        isDirAllowed = false
        isEtags = true
        welcomeFiles = listOf("index.html")
        server = jettyServer
        start()
    }

    fun handleResource(resourcePath: String, ctx: Context): Boolean {
        val resource = getResource(resourcePath) ?: return false
        resolveContentType(resource)?.let { ctx.contentType(it) }
        if (isEtags && tryHandleAsEtags(resource, ctx)) return true
        resource.newInputStream().use { ctx.result(it.readAllBytes()) }
        return true
    }

    fun getResource(path: String): Resource? = runCatching {
        baseResource?.takeIfValid(path) ?: baseResource?.takeIfValid("$path/index.html")
    }.getOrNull()

    fun Resource.takeIfValid(path: String) =
        this.resolve(path)?.takeIf { it.exists() && !it.isDirectory && isValidResource(it, path) }

    private fun isValidResource(resource: Resource, path: String) =
        !resource.isAlias || config.aliasCheck?.checkAlias(path, resource) == true

    internal fun resolveContentType(resource: Resource): String? {
        // use resource name for content type resolution (e.g., "index.html" not "/").
        // this ensures welcome files get the correct content type based on the file extension.
        val resourcePath = resource.name
        return config.mimeTypes.mapping().entries.firstOrNull { resourcePath.endsWith(".${it.key}", ignoreCase = true) }?.value
            ?: mimeTypes.getMimeByExtension(resourcePath)
    }

    internal fun tryHandleAsEtags(resource: Resource, ctx: Context): Boolean {
        val computedEtag = runCatching { computeWeakEtag(resource) }.getOrNull() ?: return false
        if (ctx.header(Header.IF_NONE_MATCH) == computedEtag) {
            ctx.status(304)
            return true
        }
        ctx.header(Header.ETAG, computedEtag)
        return false
    }

    private fun getResourceBase(config: StaticFileConfig): Resource {
        val noSuchDirMessageBuilder: (String) -> String = { "Static resource directory with path: '$it' does not exist." }
        val classpathHint = "Depending on your setup, empty folders might not get copied to classpath."

        if (config.location == Location.CLASSPATH) {
            return ResourceFactory.of(this).newClassLoaderResource(config.directory, false)
                ?: throw JavalinException("${noSuchDirMessageBuilder(config.directory)} $classpathHint")
        }
        // Use the absolute path as this aids in debugging
        val absoluteDirectoryPath = Path(config.directory).absolute().normalize()
        if (!Files.exists(absoluteDirectoryPath)) {
            throw JavalinException(noSuchDirMessageBuilder(absoluteDirectoryPath.toString()))
        }
        return ResourceFactory.of(this).newResource(config.directory)
    }

}
