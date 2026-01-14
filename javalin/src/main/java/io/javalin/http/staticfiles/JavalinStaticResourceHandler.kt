package io.javalin.http.staticfiles

import io.javalin.compression.CompressionStrategy
import io.javalin.compression.Compressor
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.router.exception.isClientAbortException
import io.javalin.security.RouteRole
import io.javalin.util.JavalinLogger
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

class JavalinStaticResourceHandler : ResourceHandler {

    private val pendingConfigs = mutableListOf<StaticFileConfig>()
    private val handlers = mutableListOf<StaticFileHandler>()
    private val precompressCache = ConcurrentHashMap<String, ByteArray>()
    private var initialized = false
    private lateinit var compressionStrategy: CompressionStrategy

    /** Returns the number of cached precompressed files (for testing) */
    fun getPrecompressCacheSize(): Int = precompressCache.size

    /** Initialize handlers and log them - called during server startup */
    override fun init(compressionStrategy: CompressionStrategy) {
        this.compressionStrategy = compressionStrategy
        pendingConfigs.forEach { addHandler(it) }
        pendingConfigs.clear()
        initialized = true
    }

    override fun addStaticFileConfig(config: StaticFileConfig): Boolean {
        if (initialized) addHandler(config) else pendingConfigs.add(config)
        return true
    }

    private fun addHandler(config: StaticFileConfig) {
        val handler = StaticFileHandler(config)
        JavalinLogger.info("Static file handler added: $config. File system location: '${handler.baseResource}'")
        handlers.add(handler)
    }

    override fun canHandle(ctx: Context): Boolean = findHandler(ctx) != null

    override fun handle(ctx: Context): Boolean {
        val (handler, resourcePath) = findHandler(ctx) ?: return false
        try {
            handler.config.headers.forEach { ctx.header(it.key, it.value) }
            return if (handler.config.precompressMaxSize > 0) {
                handlePrecompressed(resourcePath, ctx, compressionStrategy, handler)
            } else {
                handler.handleResource(resourcePath, ctx)
            }
        } catch (e: Exception) {
            // Check for client disconnect (Jetty EofException) or alias violations
            if (isClientAbortException(e) || e.message?.contains("alias") == true) {
                return false
            }
            throw e
        }
    }

    private fun findHandler(ctx: Context): Pair<StaticFileHandler, String>? {
        val target = ctx.req().requestURI.removePrefix(ctx.req().contextPath)
        return handlers.asSequence()
            .filterNot { it.config.skipFileFunction?.invoke(ctx.req()) == true }
            .filter { it.config.hostedPath == "/" || target.startsWith(it.config.hostedPath) }
            .map { it to if (it.config.hostedPath == "/") target else target.removePrefix(it.config.hostedPath) }
            .find { (handler, resourcePath) -> handler.getResource(resourcePath) != null }
    }

    override fun resourceRouteRoles(ctx: Context): Set<RouteRole> =
        findHandler(ctx)?.first?.config?.roles ?: emptySet()

    private fun handlePrecompressed(
        resourcePath: String,
        ctx: Context,
        compressionStrategy: CompressionStrategy,
        handler: StaticFileHandler
    ): Boolean {
        val resource = handler.getResource(resourcePath) ?: return false
        val contentType = handler.resolveContentType(resource, resourcePath)
        val compressor = compressionStrategy.findMatchingCompressor(ctx.header(Header.ACCEPT_ENCODING) ?: "")
            .takeIf { contentType != null && shouldCompress(contentType, compressionStrategy) }

        val resultBytes = getCachedBytes(resource, resourcePath, compressor, handler.config.precompressMaxSize) ?: return false

        ctx.header(Header.CONTENT_LENGTH, resultBytes.size.toString())
        ctx.header(Header.CONTENT_TYPE, contentType ?: "")

        if (compressor != null) {
            ctx.disableCompression()
            ctx.header(Header.CONTENT_ENCODING, compressor.encoding())
        }

        if (handler.tryHandleEtag(resource, ctx)) return true
        ctx.result(resultBytes)
        return true
    }

    private fun shouldCompress(mimeType: String, strategy: CompressionStrategy) =
        mimeType in strategy.allowedMimeTypes || strategy.excludedMimeTypes.none { mimeType.contains(it, ignoreCase = true) }

    private fun getCachedBytes(resource: StaticResource, target: String, compressor: Compressor?, maxSize: Int): ByteArray? {
        if (resource.length() > maxSize) {
            JavalinLogger.warn(
                "Static file '$target' is larger than configured max size for pre-compression ($maxSize bytes).\n" +
                    "You can configure the max size: `staticFiles.precompressMaxSize = newMaxSize`."
            )
            return null
        }
        return precompressCache.computeIfAbsent(target + (compressor?.extension() ?: "")) {
            ByteArrayOutputStream().also { output ->
                resource.newInputStream().use { input ->
                    (compressor?.compress(output) ?: output).use { input.copyTo(it) }
                }
            }.toByteArray()
        }
    }
}

