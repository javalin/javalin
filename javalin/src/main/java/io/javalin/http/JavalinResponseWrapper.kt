package io.javalin.http

import io.javalin.compression.CompressedStream
import io.javalin.config.JavalinConfig
import io.javalin.http.HandlerType.GET
import io.javalin.http.Header.CONTENT_ENCODING
import io.javalin.http.Header.ETAG
import io.javalin.http.Header.IF_NONE_MATCH
import io.javalin.http.HttpCode.NOT_MODIFIED
import io.javalin.util.Util
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import java.io.ByteArrayInputStream
import java.io.InputStream

class JavalinResponseWrapper(
    private val ctx: Context,
    private val cfg: JavalinConfig
) : HttpServletResponseWrapper(ctx.res()) {

    private val outputStreamWrapper by lazy { OutputStreamWrapper(cfg, ctx) }
    override fun getOutputStream() = outputStreamWrapper

    private val serverEtag by lazy { getHeader(ETAG) }
    private val clientEtag by lazy { ctx.req().getHeader(IF_NONE_MATCH) }

    fun write(resultStream: InputStream?) = when {
        resultStream == null -> {} // nothing to write (and nothing to close)
        serverEtag != null && serverEtag == clientEtag -> closeWith304(resultStream) // client etag matches, nothing to write
        serverEtag == null && cfg.http.generateEtags && ctx.method() == GET && resultStream is ByteArrayInputStream -> generateEtagWriteAndClose(resultStream)
        else -> writeToWrapperAndClose(resultStream)
    }

    private fun generateEtagWriteAndClose(resultStream: ByteArrayInputStream) {
        val generatedEtag = Util.getChecksumAndReset(resultStream)
        setHeader(ETAG, generatedEtag)
        when (generatedEtag) {
            clientEtag -> closeWith304(resultStream)
            else -> writeToWrapperAndClose(resultStream)
        }
    }

    private fun writeToWrapperAndClose(inputStream: InputStream) {
        inputStream.use { input ->
            outputStreamWrapper.use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun closeWith304(inputStream: InputStream) {
        inputStream.use { ctx.status(NOT_MODIFIED) }
    }

}

class OutputStreamWrapper(val cfg: JavalinConfig, val ctx: Context, val response: HttpServletResponse = ctx.res()) : ServletOutputStream() {
    private val compression = cfg.pvt.compressionStrategy
    private var compressedStream: CompressedStream? = null

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (compressedStream == null && length >= compression.minSizeForCompression && response.contentType.allowsForCompression()) {
            compressedStream = CompressedStream.tryBrotli(compression, ctx) ?: CompressedStream.tryGzip(compression, ctx)
            compressedStream?.let { response.setHeader(CONTENT_ENCODING, it.type.typeName) }
        }
        (compressedStream?.outputStream ?: response.outputStream).write(bytes, offset, length) // fall back to default stream if no compression
    }

    private fun String?.allowsForCompression(): Boolean =
        this == null || compression.excludedMimeTypesFromCompression.none { excluded -> this.contains(excluded, ignoreCase = true) }

    override fun write(byte: Int) = response.outputStream.write(byte)
    override fun setWriteListener(writeListener: WriteListener?) = response.outputStream.setWriteListener(writeListener)
    override fun isReady(): Boolean = response.outputStream.isReady
    override fun close() {
        compressedStream?.outputStream?.close()
    }
}
