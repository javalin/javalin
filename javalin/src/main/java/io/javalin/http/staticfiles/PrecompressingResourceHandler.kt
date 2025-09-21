/*
 * Javalin - https://javalin.io
 * Copyright 2017 David Åse
 * Licensed under Apache 2.0: https://github.com/tipsy/javalin/blob/master/LICENSE
 */

package io.javalin.http.staticfiles

import io.javalin.compression.CompressionStrategy
import io.javalin.compression.Compressor
import io.javalin.compression.forType
import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.util.JavalinLogger
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Native implementation of precompressing resource handler that doesn't depend on Jetty.
 * Provides static file precompression and caching functionality using only standard Java libraries.
 */
object PrecompressingResourceHandler {

    val compressedFiles = ConcurrentHashMap<String, ByteArray>()

    @JvmStatic
    fun clearCache() = compressedFiles.clear()

    @JvmField
    var resourceMaxSize: Int = 2 * 1024 * 1024 // the unit of resourceMaxSize is byte

    fun handle(target: String, resource: JavalinResource, ctx: Context, compStrat: CompressionStrategy, config: StaticFileConfig): Boolean {
        val acceptEncoding = ctx.header(Header.ACCEPT_ENCODING) ?: ""
        var compressor = findMatchingCompressor(acceptEncoding, compStrat)
        
        // Apply custom mime types from configuration first
        val customMimeType = config.mimeTypes.getMapping().entries.firstOrNull { 
            target.endsWith(".${it.key}", ignoreCase = true) 
        }?.value
        
        val contentType = customMimeType ?: getMimeTypeByExtension(target)
        
        if (contentType == null || excludedMimeType(contentType, compStrat)) {
            compressor = null
        }
        
        val resultByteArray = getStaticResourceByteArray(resource, target, compressor) ?: return false
        
        // Set headers first, before calling ctx.result()
        ctx.header(Header.CONTENT_LENGTH, resultByteArray.size.toString())
        ctx.header(Header.CONTENT_TYPE, contentType ?: "")
        
        if (compressor != null) {
            // Disable Javalin's compression since we're serving precompressed content
            ctx.disableCompression()
            // Set content-encoding header directly on Context - this should persist
            ctx.header(Header.CONTENT_ENCODING, compressor.encoding())
        }
        ctx.header(Header.IF_NONE_MATCH)?.let { requestEtag ->
            if (requestEtag == resource.weakETag) {
                ctx.status(304)
                return true // return early if resource is same as client cached version
            }
        }
        ctx.header(Header.ETAG, resource.weakETag ?: "")
        ctx.result(resultByteArray)
        return true
    }

    private fun getStaticResourceByteArray(resource: JavalinResource, target: String, compressor: Compressor?): ByteArray? {
        if (resource.length > resourceMaxSize) {
            JavalinLogger.warn(
                """
                Static file '$target' is larger than configured max size for pre-compression ($resourceMaxSize bytes).
                You can configure this with `PrecompressingResourceHandler.resourceMaxSize = newMaxSize`.
                Alternatively, you can set `precompress = false` to serve files without compression.
                """
            )
            return null
        }
        
        val cacheKey = "${target}-${compressor?.encoding() ?: "none"}"
        
        return compressedFiles.computeIfAbsent(cacheKey) {
            try {
                val inputStream = resource.newInputStream() ?: return@computeIfAbsent byteArrayOf()
                val originalBytes = inputStream.use { it.readAllBytes() }
                
                if (compressor != null) {
                    val outputStream = ByteArrayOutputStream()
                    val compressingStream = compressor.compress(outputStream)
                    compressingStream.use { it.write(originalBytes) }
                    outputStream.toByteArray()
                } else {
                    originalBytes
                }
            } catch (e: Exception) {
                JavalinLogger.warn("Failed to read/compress static file '$target'", e)
                byteArrayOf()
            }
        } ?: return null
    }

    private fun getMimeTypeByExtension(path: String): String? {
        val extension = path.substringAfterLast('.', "").lowercase()
        return mimeTypeMap[extension]
    }

    private fun excludedMimeType(mimeType: String, compressionStrategy: CompressionStrategy): Boolean = when {
        mimeType.startsWith("image/") -> true
        mimeType.startsWith("video/") -> true
        mimeType.startsWith("audio/") -> true
        mimeType.contains("font") -> true
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

    private val mimeTypeMap: Map<String, String> = mapOf(
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