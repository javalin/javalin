package io.javalin.jetty

import io.javalin.compression.CompressionStrategy
import io.javalin.compression.Compressor
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.util.JavalinLogger
import org.eclipse.jetty.util.resource.Resource
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

object JettyPrecompressingResourceHandler {

    val compressedFiles = ConcurrentHashMap<String, ByteArray>()

    @JvmStatic
    fun clearCache() = compressedFiles.clear()

    @JvmField
    var resourceMaxSize: Int = 2 * 1024 * 1024

    fun handle(resourcePath: String, ctx: Context, compressionStrategy: CompressionStrategy, handler: ConfigurableHandler): Boolean {
        val resource = handler.getResource(resourcePath) ?: return false
        val contentType = handler.resolveContentType(resourcePath)
        val compressor = compressionStrategy.findMatchingCompressor(ctx.header(Header.ACCEPT_ENCODING) ?: "")
            .takeUnless { contentType == null || excludedMimeType(contentType, compressionStrategy) }

        val resultByteArray = getCachedResourceBytes(resource, resourcePath, compressor) ?: return false

        ctx.header(Header.CONTENT_LENGTH, resultByteArray.size.toString())
        ctx.header(Header.CONTENT_TYPE, contentType ?: "")

        if (compressor != null) {
            ctx.disableCompression()
            ctx.header(Header.CONTENT_ENCODING, compressor.encoding())
        }

        if (handler.tryHandleAsEtags(resource, ctx)) return true

        ctx.result(resultByteArray)
        return true
    }

    private fun excludedMimeType(mimeType: String, compressionStrategy: CompressionStrategy) = when {
        mimeType.isEmpty() -> false
        compressionStrategy.allowedMimeTypes.contains(mimeType) -> false
        else -> compressionStrategy.excludedMimeTypes.any { excluded -> mimeType.contains(excluded, ignoreCase = true) }
    }

    private fun getCachedResourceBytes(resource: Resource, target: String, compressor: Compressor?): ByteArray? {
        if (resource.length() > resourceMaxSize) {
            JavalinLogger.warn(
                "Static file '$target' is larger than configured max size for pre-compression ($resourceMaxSize bytes).\n" +
                    "You can configure the max size with `JettyPrecompressingResourceHandler.resourceMaxSize = newMaxSize`."
            )
            return null
        }
        return compressedFiles.computeIfAbsent(target + (compressor?.extension() ?: "")) {
            ByteArrayOutputStream().also { output ->
                resource.newInputStream().use { input ->
                    (compressor?.compress(output) ?: output).use { input.copyTo(it) }
                }
            }.toByteArray()
        }
    }

}
