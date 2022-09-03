package io.javalin.jetty

import io.javalin.compression.CompressionStrategy
import io.javalin.compression.CompressionType
import io.javalin.compression.LeveledBrotliStream
import io.javalin.compression.LeveledGzipStream
import io.javalin.http.Header
import io.javalin.util.CoreDependency
import io.javalin.util.JavalinLogger
import io.javalin.util.Util
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.http.MimeTypes
import org.eclipse.jetty.util.resource.Resource
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

object JettyPrecompressingResourceHandler {

    val compressedFiles = ConcurrentHashMap<String, ByteArray>()

    @JvmField
    var resourceMaxSize: Int = 2 * 1024 * 1024 // the unit of resourceMaxSize is byte

    val excludedMimeTypes = CompressionStrategy().excludedMimeTypesFromCompression

    fun handle(target: String, resource: Resource, req: HttpServletRequest, res: HttpServletResponse): Boolean {
        if (resource.exists() && !resource.isDirectory) {
            var acceptCompressType = CompressionType.getByAcceptEncoding(req.getHeader(Header.ACCEPT_ENCODING) ?: "")
            val contentType = MimeTypes.getDefaultMimeByExtension(target) // get content type by file extension
            if (contentType == null || excludedMimeType(contentType)) {
                acceptCompressType = CompressionType.NONE
            }
            val resultByteArray = getStaticResourceByteArray(resource, target, acceptCompressType) ?: return false
            res.setContentLength(resultByteArray.size)
            res.setHeader(Header.CONTENT_TYPE, contentType)

            if (acceptCompressType != CompressionType.NONE)
                res.setHeader(Header.CONTENT_ENCODING, acceptCompressType.typeName)

            val weakETag = resource.weakETag // jetty resource use weakETag too
            req.getHeader(Header.IF_NONE_MATCH)?.let { etag ->
                if (etag == weakETag) {
                    res.status = 304
                    return true
                }
            }
            res.setHeader(Header.ETAG, weakETag)
            resultByteArray.inputStream().copyTo(res.outputStream)
            res.outputStream.close()
            return true
        }
        return false
    }

    private fun getStaticResourceByteArray(resource: Resource, target: String, type: CompressionType): ByteArray? {
        if (resource.length() > resourceMaxSize) {
            JavalinLogger.warn(
                "Static file '$target' is larger than configured max size for pre-compression ($resourceMaxSize bytes).\n" +
                    "You can configure the max size with `JettyPrecompressingResourceHandler.resourceMaxSize = newMaxSize`."
            )
            return null
        }
        return compressedFiles.computeIfAbsent(target + type.extension) { getCompressedByteArray(resource, type) }
    }

    private val brotliAvailable = Util.classExists(CoreDependency.JVMBROTLI.testClass)
    private fun getCompressedByteArray(resource: Resource, type: CompressionType): ByteArray {
        val fileInput = resource.inputStream
        val byteArrayOutputStream = ByteArrayOutputStream()
        val outputStream: OutputStream = when {
            type == CompressionType.GZIP -> {
                LeveledGzipStream(byteArrayOutputStream, 9) // use max-level compression
            }
            type == CompressionType.BR && brotliAvailable -> {
                LeveledBrotliStream(byteArrayOutputStream, 11) // use max-level compression
            }
            else -> byteArrayOutputStream
        }
        fileInput.copyTo(outputStream)
        fileInput.close()
        outputStream.close()
        return byteArrayOutputStream.toByteArray()
    }

    private fun excludedMimeType(mimeType: String) =
        if (mimeType == "") false else excludedMimeTypes.any { excluded -> mimeType.contains(excluded, ignoreCase = true) }

}
