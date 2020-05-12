package io.javalin.http.staticfiles

import io.javalin.core.compression.CompressionStrategy
import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.LeveledBrotliStream
import io.javalin.http.LeveledGzipStream
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.util.resource.Resource
import java.io.*


class PrecompressingResourceHandler(val compStrat: CompressionStrategy) {

    val compressedFiles = HashMap<String, ByteArray>()

    companion object {
        val excludedMimeTypes = setOf(
                "image/",
                "audio/",
                "video/",
                "application/compress",
                "application/zip",
                "application/gzip",
                "application/bzip2",
                "application/brotli",
                "application/x-xz",
                "application/x-rar-compressed")
    }

    fun handle(resource: Resource, ctx: Context): Boolean {
        if (resource.exists() && !resource.isDirectory) {
            var acceptCompressType = CompressType.getByAcceptEncoding(ctx.header(Header.ACCEPT_ENCODING) ?: "")
            val contentType = MimeTypes.getDefaultMimeByExtension(resource.file.name)//get content type by file extension
            val resourceCompressible = !excludedMimeType(contentType)
            if (!resourceCompressible)
                acceptCompressType = CompressType.NONE
            val resultByteArray = getStaticResourceByteArray(resource, ctx.path(), acceptCompressType)

            //TODO etag
            //TODO request range and content length

            ctx.header(Header.CONTENT_TYPE, contentType)
            ctx.header(Header.CONTENT_ENCODING, acceptCompressType.typeName)
            ctx.result(resultByteArray)

            return true
        }
        return false
    }

    private fun getStaticResourceByteArray(resource: Resource, target: String, type: CompressType): ByteArray {
        if(resource.length() > Int.MAX_VALUE)
            throw RuntimeException("Precompression doesn't support static file size large than 2GB")
        val key = target + type.extension
        if (!compressedFiles.containsKey(key)) {
            compressedFiles[key] = getCompressedByteArray(resource, type)
        }
        return compressedFiles[key]!!
    }

    private fun getCompressedByteArray(resource: Resource, type: CompressType): ByteArray {
        val fileInput = resource.file.inputStream()
        val byteArrayOutputStream = ByteArrayOutputStream()
        val outputStream: OutputStream = when {
            compStrat.gzip != null && type == CompressType.GZIP -> {
                LeveledGzipStream(byteArrayOutputStream, compStrat.gzip.level)
            }
            compStrat.brotli != null && type == CompressType.BR -> {
                LeveledBrotliStream(byteArrayOutputStream, compStrat.brotli.level)
            }
            else ->
                byteArrayOutputStream
        }
        fileInput.copyTo(outputStream, bufferSize = 2048)
        fileInput.close()
        outputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    private fun excludedMimeType(mimeType: String) =
            if (mimeType == "") false else excludedMimeTypes.any { excluded -> mimeType.contains(excluded, ignoreCase = true) }

    enum class CompressType(val typeName: String, val extension: String) {
        GZIP("gzip", ".gz"),
        BR("br", ".br"),
        NONE("", "");

        fun acceptEncoding(acceptEncoding: String): Boolean {
            return acceptEncoding.contains(typeName, ignoreCase = true)
        }

        companion object {
            fun getByAcceptEncoding(acceptEncoding: String): CompressType {
                values().forEach {
                    if (it.acceptEncoding(acceptEncoding)) return it
                }
                return NONE
            }
        }
    }
}
