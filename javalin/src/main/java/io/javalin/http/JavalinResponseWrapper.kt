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
import io.javalin.http.HttpCode.NOT_MODIFIED
import io.javalin.jetty.JettyPrecompressingResourceHandler.CompressType
import io.javalin.jetty.JettyPrecompressingResourceHandler.CompressType.BR
import io.javalin.jetty.JettyPrecompressingResourceHandler.CompressType.GZIP
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream
import javax.servlet.ServletOutputStream
import javax.servlet.WriteListener
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

class JavalinResponseWrapper(private val ctx: Context, private val config: JavalinConfig, private val requestType: HandlerType) : HttpServletResponseWrapper(ctx.res) {

    private val outputStreamWrapper by lazy { OutputStreamWrapper(config, ctx.req, ctx.res) }
    override fun getOutputStream() = outputStreamWrapper

    private val serverEtag by lazy { getHeader(ETAG) }
    private val clientEtag by lazy { ctx.req.getHeader(IF_NONE_MATCH) }

    fun write(resultStream: InputStream?) = when {
        resultStream == null -> {} // nothing to write (and nothing to close)
        serverEtag != null && serverEtag == clientEtag -> closeWith304(resultStream) // client etag matches, nothing to write
        serverEtag == null && requestType == GET && config.autogenerateEtags -> generateEtagWriteAndClose(resultStream)
        else -> writeToWrapperAndClose(resultStream)
    }

    private fun generateEtagWriteAndClose(resultStream: InputStream) {
        val inputStream = resultStream.use { it.readBytes().inputStream() } // TODO: https://github.com/tipsy/javalin/issues/1505
        val generatedEtag = Util.getChecksumAndReset(inputStream)
        setHeader(ETAG, generatedEtag)
        when (generatedEtag) {
            clientEtag -> closeWith304(inputStream)
            else -> writeToWrapperAndClose(inputStream)
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

class OutputStreamWrapper(val config: JavalinConfig, val request: HttpServletRequest, val response: HttpServletResponse) : ServletOutputStream() {
    private val compression = config.inner.compressionStrategy
    private var compressedStream: CompressedStream? = null

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (compressedStream == null && length >= compression.minSizeForCompression && isAllowedForCompression()) {
            compressedStream = tryCreateBrotliStream() ?: tryCreateGzipStream()
            compressedStream?.apply { response.setHeader(CONTENT_ENCODING, type.typeName) }
        }
        (compressedStream?.outputStream ?: response.outputStream).write(bytes, offset, length) // fall back to default stream if no compression
    }

    fun tryCreateBrotliStream(): CompressedStream? =
        if (compression.brotli != null && request.getHeader(ACCEPT_ENCODING)?.contains(BR.typeName, ignoreCase = true) == true)
            CompressedStream(BR, LeveledBrotliStream(response.outputStream, compression.brotli.level))
        else null

    fun tryCreateGzipStream(): CompressedStream? =
        if (compression.gzip != null && request.getHeader(ACCEPT_ENCODING)?.contains(GZIP.typeName, ignoreCase = true) == true)
            CompressedStream(GZIP, LeveledGzipStream(response.outputStream, compression.gzip.level))
        else null

    private fun isAllowedForCompression(): Boolean =
        response.contentType?.let { compression.excludedMimeTypesFromCompression.none { excluded -> it.contains(excluded, ignoreCase = true) } } ?: true

    override fun write(byte: Int) = response.outputStream.write(byte)
    override fun setWriteListener(writeListener: WriteListener?) = response.outputStream.setWriteListener(writeListener)
    override fun isReady(): Boolean = response.outputStream.isReady
    override fun close() { compressedStream?.outputStream?.close() }
}

data class CompressedStream(val type: CompressType, val outputStream: OutputStream)

class LeveledGzipStream(out: OutputStream, level: Int) : GZIPOutputStream(out) {
    init {
        this.def.setLevel(level)
    }
}

class LeveledBrotliStream(out: OutputStream, level: Int) : BrotliOutputStream(out, Encoder.Parameters().setQuality(level))
