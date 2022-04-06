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
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

class JavalinResponseWrapper(private val ctx: Context, private val config: JavalinConfig, private val requestType: HandlerType) : HttpServletResponseWrapper(ctx.res) {

    private val outputStreamWrapper by lazy { OutputStreamWrapper(ctx, config) }
    override fun getOutputStream() = outputStreamWrapper

    private val outputStreamWrapper by lazy { OutputStreamWrapper(config, ctx.req, ctx.res) }

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

    override fun getOutputStream() = outputStreamWrapper
}

class OutputStreamWrapper(val config: JavalinConfig, val request: HttpServletRequest, val response: HttpServletResponse) : ServletOutputStream() {
    private val compression = config.inner.compressionStrategy
    private var compressedStream: CompressedStream? = null

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (compressedStream == null && length >= config.minSizeForCompression && response.isAllowedForCompression(config.excludedMimeTypesFromCompression)) {
            compressedStream = createBrotliStream() ?: createGzipStream()
            compressedStream?.run { response.setHeader(CONTENT_ENCODING, name) }
        }
        (compressedStream?.outputStream ?: response.outputStream).write(bytes, offset, length) // fall back to default stream if no compression
    }

    fun createBrotliStream(): CompressedStream? =
        if (compression.brotli != null && request.getHeader(ACCEPT_ENCODING)?.contains(BR, ignoreCase = true) == true)
            CompressedStream(BR, LeveledBrotliStream(response.outputStream, compression.brotli.level))
        else null

    fun createGzipStream(): CompressedStream? =
        if (compression.gzip != null && request.getHeader(ACCEPT_ENCODING)?.contains(GZIP, ignoreCase = true) == true)
            CompressedStream(GZIP, LeveledGzipStream(response.outputStream, compression.gzip.level))
        else null

    override fun write(byte: Int) = response.outputStream.write(byte)
    override fun setWriteListener(writeListener: WriteListener?) = response.outputStream.setWriteListener(writeListener)
    override fun isReady(): Boolean = response.outputStream.isReady
    override fun close() { compressedStream?.outputStream?.close() }
}

private fun HttpServletResponse.isAllowedForCompression(excludedMimeTypesFromCompression: Collection<String>): Boolean =
    contentType?.let { excludedMimeTypesFromCompression.none { excluded -> it.contains(excluded, ignoreCase = true) } } ?: true

data class CompressedStream(val name: String, val outputStream: OutputStream)

private const val GZIP = "gzip"
class LeveledGzipStream(out: OutputStream, level: Int) : GZIPOutputStream(out) {
    init {
        this.def.setLevel(level)
    }
}

private const val BR = "br"
class LeveledBrotliStream(out: OutputStream, level: Int) : BrotliOutputStream(out, Encoder.Parameters().setQuality(level))
