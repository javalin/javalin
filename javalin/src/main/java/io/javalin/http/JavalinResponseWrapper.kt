package io.javalin.http

import com.nixxcode.jvmbrotli.enc.BrotliOutputStream
import com.nixxcode.jvmbrotli.enc.Encoder
import io.javalin.core.JavalinConfig
import io.javalin.core.util.Header.ACCEPT_ENCODING
import io.javalin.core.util.Header.CONTENT_ENCODING
import io.javalin.core.util.Header.ETAG
import io.javalin.core.util.Header.IF_NONE_MATCH
import io.javalin.core.util.Util
import io.javalin.http.HandlerType.GET
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream
import javax.servlet.ServletOutputStream
import javax.servlet.WriteListener
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

class JavalinResponseWrapper(private val ctx: Context, private val config: JavalinConfig, private val requestType: HandlerType) : HttpServletResponseWrapper(ctx.res) {

    private val outputStreamWrapper by lazy { OutputStreamWrapper(ctx, config) }
    override fun getOutputStream() = outputStreamWrapper

    private val serverEtag by lazy { ctx.res.getHeader(ETAG) }
    private val clientEtag by lazy { ctx.req.getHeader(IF_NONE_MATCH) }

    fun write(resultStream: InputStream?) = when {
        resultStream == null -> {} // nothing to write (and nothing to close)
        serverEtag != null && serverEtag == clientEtag -> closeWith304(resultStream) // client etag matches, nothing to write
        serverEtag == null && requestType == GET && config.autogenerateEtags -> generateEtagWriteAndClose(resultStream)
        else -> writeToWrapperAndClose(resultStream)
    }

    private fun generateEtagWriteAndClose(resultStream: InputStream) {
        // TODO: https://github.com/tipsy/javalin/issues/1505
        val inputStream = resultStream.readBytes().inputStream().also { resultStream.close() }
        val generatedEtag = Util.getChecksumAndReset(inputStream)
        ctx.res.setHeader(ETAG, generatedEtag)
        when (generatedEtag) {
            clientEtag -> closeWith304(inputStream)
            else -> writeToWrapperAndClose(inputStream)
        }
    }

    private fun writeToWrapperAndClose(inputStream: InputStream) {
        inputStream.copyTo(outputStreamWrapper).also { inputStream.close() }
        outputStreamWrapper.finalizeCompression()
    }

    private fun closeWith304(inputStream: InputStream) = inputStream.close().also { ctx.res.status = 304 }

}

private const val BR = "br"
private const val GZIP = "gzip"

class OutputStreamWrapper(private val ctx: Context, config: JavalinConfig) : ServletOutputStream() {

    private val acceptsBrotli by lazy { ctx.req.getHeader(ACCEPT_ENCODING)?.contains(BR, ignoreCase = true) == true }
    private val acceptsGzip by lazy { ctx.req.getHeader(ACCEPT_ENCODING)?.contains(GZIP, ignoreCase = true) == true }
    private val compression = config.inner.compressionStrategy
    private val response: HttpServletResponse by lazy { ctx.res }
    private val isAllowedType by lazy { response.contentType == null || excludedMimeTypes.none { response.contentType.contains(it, ignoreCase = true) } }

    private var compressedStream: OutputStream? = null

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (isAllowedType && compressedStream == null && length >= minSizeForCompression) {
            if (acceptsBrotli && compression.brotli != null) {
                response.setHeader(CONTENT_ENCODING, BR)
                compressedStream = LeveledBrotliStream(response.outputStream, compression.brotli.level)
            } else if (acceptsGzip && compression.gzip != null) {
                response.setHeader(CONTENT_ENCODING, GZIP)
                compressedStream = LeveledGzipStream(response.outputStream, compression.gzip.level)
            }
        }
        (compressedStream ?: response.outputStream).write(bytes, offset, length) // fall back to default stream if no compression
    }

    fun finalizeCompression() = compressedStream?.close()

    override fun isReady(): Boolean = response.outputStream.isReady
    override fun setWriteListener(writeListener: WriteListener?) = response.outputStream.setWriteListener(writeListener)
    override fun write(byte: Int) = response.outputStream.write(byte)

    companion object {
        var minSizeForCompression = 1500 // 1500 is the size of a packet, compressing responses smaller than this serves no purpose
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
            "application/x-rar-compressed"
        )
    }
}

class LeveledGzipStream(out: OutputStream, level: Int) : GZIPOutputStream(out) {
    init {
        this.def.setLevel(level)
    }
}

class LeveledBrotliStream(out: OutputStream, level: Int) : BrotliOutputStream(out, Encoder.Parameters().setQuality(level))
