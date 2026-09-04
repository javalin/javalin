package io.javalin.jetty

import io.javalin.compression.CompressionStrategy
import io.javalin.compression.Compressor
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.util.JavalinLogger
import org.eclipse.jetty.util.resource.Resource
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

class JettyPrecompressingResourceHandler {

    private val compressedFiles = ConcurrentHashMap<String, ByteArray>()

    internal fun getCacheSize() = compressedFiles.size

    fun handle(resourcePath: String, ctx: Context, compressionStrategy: CompressionStrategy, handler: ConfigurableHandler): Boolean {
        val resource = handler.getResource(resourcePath) ?: return false
        if (resource.length() > handler.config.precompressMaxSize) {
            JavalinLogger.warn(
                "Static file '$resourcePath' is larger than configured max size for pre-compression (${handler.config.precompressMaxSize} bytes).\n" +
                    "You can configure the max size in the static files config: `staticFiles.precompressMaxSize = newMaxSize`."
            )
            return handler.handleResource(resourcePath, ctx)
        }
        val contentType = handler.resolveContentType(resource, resourcePath)
        val preCompressor = compressionStrategy.findMatchingCompressor(ctx.header(Header.ACCEPT_ENCODING) ?: "")
            .takeUnless { contentType == null || excludedMimeType(contentType, compressionStrategy) }

        val resultByteArray = getCachedResourceBytes(resource, resourcePath, preCompressor)

        ctx.header(Header.CONTENT_LENGTH, resultByteArray.size.toString())
        ctx.header(Header.CONTENT_TYPE, contentType ?: "")

        ctx.disableCompression() // resultByteArray and Content-Length above are final, don't let the dynamic compressor touch them
        preCompressor?.let { ctx.header(Header.CONTENT_ENCODING, it.encoding()) }

        if (handler.tryHandleAsEtags(resource, ctx)) return true

        ctx.result(resultByteArray)
        return true
    }

    private fun excludedMimeType(mimeType: String, compressionStrategy: CompressionStrategy) = when {
        mimeType.isEmpty() -> false
        compressionStrategy.allowedMimeTypes.contains(mimeType) -> false
        else -> compressionStrategy.excludedMimeTypes.any { excluded -> mimeType.contains(excluded, ignoreCase = true) }
    }

    private fun getCachedResourceBytes(resource: Resource, target: String, preCompressor: Compressor?): ByteArray {
        return compressedFiles.computeIfAbsent(target + (preCompressor?.extension() ?: "")) {
            ByteArrayOutputStream().also { output ->
                resource.newInputStream().use { input ->
                    (preCompressor?.compress(output) ?: output).use { input.copyTo(it) }
                }
            }.toByteArray()
        }
    }

}
