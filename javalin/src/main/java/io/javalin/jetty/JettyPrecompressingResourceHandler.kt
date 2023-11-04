package io.javalin.jetty

import io.javalin.compression.CompressionStrategy
import io.javalin.compression.Compressor
import io.javalin.compression.forType
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.util.JavalinLogger
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.util.resource.Resource
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

object JettyPrecompressingResourceHandler {

    val compressedFiles = ConcurrentHashMap<String, ByteArray>()

    @JvmStatic
    fun clearCache() = compressedFiles.clear()

    @JvmField
    var resourceMaxSize: Int = 2 * 1024 * 1024 // the unit of resourceMaxSize is byte

    fun handle(target: String, resource: Resource, ctx: Context, compStrat: CompressionStrategy): Boolean {
        var compressor = findMatchingCompressor(ctx.header(Header.ACCEPT_ENCODING) ?: "", compStrat)
        val contentType = MimeTypes.getDefaultMimeByExtension(target) // get content type by file extension
        if (contentType == null || excludedMimeType(contentType, compStrat)) {
            compressor = null
        }
        val resultByteArray = getStaticResourceByteArray(resource, target, compressor) ?: return false
        if (compressor != null) {
            ctx.header(Header.CONTENT_ENCODING, compressor.encoding())
        }
        ctx.header(Header.CONTENT_LENGTH, resultByteArray.size.toString())
        ctx.header(Header.CONTENT_TYPE, contentType ?: "")
        ctx.header(Header.IF_NONE_MATCH)?.let { requestEtag ->
            if (requestEtag == resource.weakETag) { // jetty resource use weakETag too
                ctx.status(304)
                return true // return early if resource is same as client cached version
            }
        }
        ctx.header(Header.ETAG, resource.weakETag)
        ctx.result(resultByteArray)
        return true
    }

    private fun getStaticResourceByteArray(resource: Resource, target: String, compressor: Compressor?): ByteArray? {
        if (resource.length() > resourceMaxSize) {
            JavalinLogger.warn(
                "Static file '$target' is larger than configured max size for pre-compression ($resourceMaxSize bytes).\n" +
                    "You can configure the max size with `JettyPrecompressingResourceHandler.resourceMaxSize = newMaxSize`."
            )
            return null
        }
        val ext = compressor?.extension() ?: ""
        return compressedFiles.computeIfAbsent(target + ext) { getCompressedByteArray(resource, compressor) }
    }

    private fun getCompressedByteArray(resource: Resource, compressor: Compressor?): ByteArray {
        val fileInput = resource.inputStream
        val byteArrayOutputStream = ByteArrayOutputStream()
        val outputStream: OutputStream =
            compressor?.compress(byteArrayOutputStream)
                ?: byteArrayOutputStream

        fileInput.copyTo(outputStream)
        fileInput.close()
        outputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    private fun excludedMimeType(mimeType: String, compressionStrategy: CompressionStrategy) = when {
        mimeType == "" -> false
        compressionStrategy.allowedMimeTypes.contains(mimeType) -> false
        else -> compressionStrategy.excludedMimeTypes.any { excluded -> mimeType.contains(excluded, ignoreCase = true) }
    }

    private fun findMatchingCompressor(
        contentTypeHeader: String,
        compressionStrategy: CompressionStrategy
    ): Compressor? =
        contentTypeHeader
            .split(",")
            .map { it.trim() }
            .firstNotNullOfOrNull { compressionStrategy.compressors.forType(it) }
}
