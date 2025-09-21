/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.staticfiles

import io.javalin.config.PrivateConfig
import io.javalin.http.Context
import io.javalin.security.RouteRole
import io.javalin.util.JavalinException
import io.javalin.util.JavalinLogger
import jakarta.servlet.http.HttpServletRequest
import java.io.*
import java.net.URL
import java.net.URLConnection
import java.nio.file.*
import java.security.MessageDigest
import java.util.*

/**
 * Native implementation of ResourceHandler that doesn't depend on Jetty or Jakarta servlet APIs.
 * Provides static file serving functionality using only standard Java libraries.
 */
class NativeResourceHandler(val pvt: PrivateConfig) : ResourceHandler {

    private val lateInitConfigs = mutableListOf<StaticFileConfig>()
    private val handlers = mutableListOf<NativeConfigurableHandler>()
    private var initialized = false

    fun init() {
        handlers.addAll(lateInitConfigs.map { NativeConfigurableHandler(it) })
        lateInitConfigs.clear()
        initialized = true
    }

    override fun addStaticFileConfig(config: StaticFileConfig): Boolean {
        return if (handlers.any { it.config == config } || lateInitConfigs.any { it == config }) {
            false
        } else {
            if (initialized) {
                handlers.add(NativeConfigurableHandler(config))
            } else {
                lateInitConfigs.add(config)
            }
            true
        }
    }

    override fun canHandle(ctx: Context) = findHandler(ctx) != null

    override fun handle(ctx: Context): Boolean {
        val (handler, resourcePath) = findHandler(ctx) ?: return false
        
        try {
            // Apply custom headers
            handler.config.headers.forEach { ctx.header(it.key, it.value) }
            
            return if (handler.config.precompress) {
                val resource = handler.getResource(resourcePath)
                if (resource != null) {
                    NativePrecompressingResourceHandler.handle(resourcePath, resource, ctx, pvt.compressionStrategy, handler.config)
                } else {
                    false
                }
            } else {
                handler.handleResource(resourcePath, ctx)
            }
        } catch (e: Exception) {
            if (e.message?.contains("alias") == true) return false
            throw e
        }
    }

    private fun findHandler(ctx: Context): Pair<NativeConfigurableHandler, String>? {
        val target = ctx.req().requestURI.removePrefix(ctx.req().contextPath)
        
        // Search in active handlers first
        for (handler in handlers) {
            if (!handler.config.skipFileFunction(ctx.req())) {
                val hostedPath = handler.config.hostedPath
                val resourcePath = when {
                    hostedPath == "/" -> target
                    target.startsWith(hostedPath) -> target.removePrefix(hostedPath).removePrefix("/")
                    else -> null
                }
                if (resourcePath != null && handler.getResource(resourcePath) != null) {
                    return handler to resourcePath
                }
            }
        }
        
        // Search in late init configs if no handler found
        for (config in lateInitConfigs) {
            if (!config.skipFileFunction(ctx.req())) {
                val hostedPath = config.hostedPath
                val resourcePath = when {
                    hostedPath == "/" -> target
                    target.startsWith(hostedPath) -> target.removePrefix(hostedPath).removePrefix("/")
                    else -> null
                }
                if (resourcePath != null) {
                    val tempHandler = NativeConfigurableHandler(config)
                    if (tempHandler.getResource(resourcePath) != null) {
                        return tempHandler to resourcePath
                    }
                }
            }
        }
        
        return null
    }

    override fun getResourceRouteRoles(ctx: Context): Set<RouteRole> =
        findHandler(ctx)?.first?.config?.roles ?: emptySet()
}

/**
 * Native resource abstraction that doesn't depend on Jetty's Resource class
 */
class NativeResource(
    val path: String,
    val location: Location,
    val inputStreamProvider: () -> InputStream?,
    val exists: Boolean,
    val isDirectory: Boolean,
    val length: Long,
    val lastModified: Long,
    val isAlias: Boolean = false
) {
    fun newInputStream(): InputStream? = inputStreamProvider()
    
    val weakETag: String? by lazy {
        if (!exists) null
        else {
            try {
                val digest = MessageDigest.getInstance("MD5")
                digest.update(path.toByteArray())
                digest.update(length.toString().toByteArray())
                digest.update(lastModified.toString().toByteArray())
                "W/\"${digest.digest().joinToString("") { "%02x".format(it) }}\""
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Native configurable handler that doesn't extend Jetty's ResourceHandler
 */
class NativeConfigurableHandler(val config: StaticFileConfig) {

    private val mimeTypeMap = createMimeTypeMap()
    private val baseResource: NativeResource?

    init {
        JavalinLogger.info("Static file handler added: ${config.refinedToString()}. File system location: '${getResourceBasePath(config)}'")
        baseResource = getResourceBase(config)
    }

    fun getResource(path: String): NativeResource? {
        return try {
            baseResource ?: return null
            
            // Try to resolve the direct resource first
            resolveResource(path)?.let { resource ->
                if (resource.exists && !resource.isDirectory && isValidResource(resource, path)) {
                    return resource
                }
            }
            
            // Check for welcome file (index.html) - try multiple variations
            val welcomePaths = listOf(
                "${path.removeSuffix("/")}/index.html",
                "${path}/index.html".removePrefix("//")
            ).distinct()
            
            for (welcomePath in welcomePaths) {
                resolveResource(welcomePath)?.let { welcomeResource ->
                    if (welcomeResource.exists && !welcomeResource.isDirectory && isValidResource(welcomeResource, welcomePath)) {
                        return welcomeResource
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun resolveResource(path: String): NativeResource? {
        return when (config.location) {
            Location.CLASSPATH -> resolveClasspathResource(path)
            Location.EXTERNAL -> resolveExternalResource(path)
        }
    }
    
    private fun resolveClasspathResource(path: String): NativeResource? {
        val fullPath = "${config.directory.removeSuffix("/")}/$path".removePrefix("/")
        val url = this::class.java.classLoader.getResource(fullPath) ?: return null
        
        return try {
            val connection = url.openConnection()
            
            // Check if URL points to a directory by examining the URL path
            val isDirectory = url.path.endsWith("/") || 
                              java.io.File(url.path).isDirectory() ||
                              // Try to access it as a directory by checking if we can find index.html inside
                              (this::class.java.classLoader.getResource("$fullPath/index.html") != null)
            
            NativeResource(
                path = fullPath,
                location = Location.CLASSPATH,
                inputStreamProvider = { if (!isDirectory) url.openStream() else null },
                exists = true,
                isDirectory = isDirectory,
                length = if (!isDirectory) connection.contentLengthLong.let { if (it == -1L) 0L else it } else 0L,
                lastModified = connection.lastModified,
                isAlias = false // Classpath resources don't have aliases
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun resolveExternalResource(path: String): NativeResource? {
        val basePath = Paths.get(config.directory).toAbsolutePath().normalize()
        val resourcePath = basePath.resolve(path).normalize()
        
        // Security check: ensure the resolved path is still under the base directory
        if (!resourcePath.startsWith(basePath)) {
            return null
        }
        
        return try {
            val exists = Files.exists(resourcePath)
            val isDirectory = Files.isDirectory(resourcePath)
            val length = if (exists && !isDirectory) Files.size(resourcePath) else 0L
            val lastModified = if (exists) Files.getLastModifiedTime(resourcePath).toMillis() else 0L
            val isAlias = Files.isSymbolicLink(resourcePath)
            
            NativeResource(
                path = path,
                location = Location.EXTERNAL,
                inputStreamProvider = { if (exists && !isDirectory) Files.newInputStream(resourcePath) else null },
                exists = exists,
                isDirectory = isDirectory,
                length = length,
                lastModified = lastModified,
                isAlias = isAlias
            )
        } catch (e: Exception) {
            null
        }
    }
    
    private fun isValidResource(resource: NativeResource, path: String): Boolean {
        if (!resource.isAlias) return true
        
        // No alias check configured - default is to block all aliases for security
        if (config.nativeAliasCheck == null) return false
        
        // Apply the configured alias check
        return config.nativeAliasCheck!!.checkAlias(path, resource)
    }

    fun handleResource(resourcePath: String, ctx: Context): Boolean {
        val resource = getResource(resourcePath) ?: return false
        
        // Use native content type resolution with custom override support
        val contentType = config.mimeTypes.getMapping().entries
            .firstOrNull { resourcePath.endsWith(".${it.key}", ignoreCase = true) }?.value
            ?: getMimeTypeByExtension(resourcePath)
        
        contentType?.let { ctx.contentType(it) }
        
        // Use native ETag support
        val etag = resource.weakETag
        if (etag != null) {
            // Handle conditional requests
            if (ctx.header("If-None-Match") == etag) {
                ctx.status(304)
                return true
            }
            ctx.header("ETag", etag)
        }
        
        // Serve the resource content
        val inputStream = resource.newInputStream() ?: return false
        try {
            ctx.result(inputStream.readAllBytes())
            return true
        } finally {
            inputStream.close()
        }
    }

    private fun getMimeTypeByExtension(path: String): String? {
        val extension = path.substringAfterLast('.', "").lowercase()
        return mimeTypeMap[extension]
    }

    private fun getResourceBasePath(config: StaticFileConfig): String {
        return when (config.location) {
            Location.CLASSPATH -> "classpath:${config.directory}"
            Location.EXTERNAL -> Paths.get(config.directory).toAbsolutePath().toString()
        }
    }

    private fun getResourceBase(config: StaticFileConfig): NativeResource? {
        val noSuchDirMessageBuilder: (String) -> String = { "Static resource directory with path: '$it' does not exist." }
        val classpathHint = "Depending on your setup, empty folders might not get copied to classpath."
        
        return when (config.location) {
            Location.CLASSPATH -> {
                val url = this::class.java.classLoader.getResource(config.directory.removePrefix("/"))
                if (url == null) {
                    throw JavalinException("${noSuchDirMessageBuilder(config.directory)} $classpathHint")
                }
                
                NativeResource(
                    path = config.directory,
                    location = Location.CLASSPATH,
                    inputStreamProvider = { null }, // Directory, no stream
                    exists = true,
                    isDirectory = true,
                    length = 0L,
                    lastModified = 0L,
                    isAlias = false
                )
            }
            Location.EXTERNAL -> {
                val absoluteDirectoryPath = Paths.get(config.directory).toAbsolutePath().normalize()
                if (!Files.exists(absoluteDirectoryPath)) {
                    throw JavalinException(noSuchDirMessageBuilder(absoluteDirectoryPath.toString()))
                }
                
                NativeResource(
                    path = absoluteDirectoryPath.toString(),
                    location = Location.EXTERNAL,
                    inputStreamProvider = { null }, // Directory, no stream
                    exists = true,
                    isDirectory = true,
                    length = 0L,
                    lastModified = Files.getLastModifiedTime(absoluteDirectoryPath).toMillis(),
                    isAlias = Files.isSymbolicLink(absoluteDirectoryPath)
                )
            }
        }
    }

    private fun createMimeTypeMap(): Map<String, String> {
        return mapOf(
            // Common web content types
            "html" to "text/html",
            "htm" to "text/html",
            "css" to "text/css",
            "js" to "text/javascript",
            "mjs" to "text/javascript",
            "json" to "application/json",
            "xml" to "application/xml",
            "txt" to "text/plain",
            
            // Images
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "gif" to "image/gif",
            "svg" to "image/svg+xml",
            "ico" to "image/x-icon",
            "webp" to "image/webp",
            
            // Fonts
            "woff" to "font/woff",
            "woff2" to "font/woff2",
            "ttf" to "font/ttf",
            "otf" to "font/otf",
            "eot" to "application/vnd.ms-fontobject",
            
            // Documents
            "pdf" to "application/pdf",
            "doc" to "application/msword",
            "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            
            // Archives
            "zip" to "application/zip",
            "tar" to "application/x-tar",
            "gz" to "application/gzip",
            
            // Audio/Video
            "mp3" to "audio/mpeg",
            "mp4" to "video/mp4",
            "webm" to "video/webm",
            "ogg" to "audio/ogg",
            
            // Other
            "manifest" to "text/cache-manifest",
            "appcache" to "text/cache-manifest"
        )
    }
}