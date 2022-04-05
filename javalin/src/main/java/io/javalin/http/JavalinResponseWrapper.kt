package io.javalin.http

import com.nixxcode.jvmbrotli.enc.BrotliOutputStream
import com.nixxcode.jvmbrotli.enc.Encoder
import io.javalin.core.JavalinConfig
import io.javalin.core.util.Header.ACCEPT_ENCODING
import io.javalin.core.util.Header.CONTENT_ENCODING
import io.javalin.core.util.Header.ETAG
import io.javalin.core.util.Header.IF_NONE_MATCH
import io.javalin.core.util.Util
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream
import javax.servlet.ServletOutputStream
import javax.servlet.WriteListener
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

class JavalinResponseWrapper(private val ctx: Context, private val config: JavalinConfig, private val requestType: HandlerType) : HttpServletResponseWrapper(ctx.res) {
    private val response: HttpServletResponse by lazy { ctx.res }
    private val outputStreamWrapper by lazy { OutputStreamWrapper(ctx, config) }
    override fun getOutputStream() = outputStreamWrapper

    fun write(resultStream: InputStream?) {
        if (resultStream == null) return
        var inputStream = resultStream
        if (response.getHeader(ETAG) != null || (config.autogenerateEtags && requestType == HandlerType.GET)) {
            val serverEtag: String
            if (response.getHeader(ETAG) != null) {
                serverEtag = response.getHeader(ETAG)
            } else {
                //we must load and wrap the whole input stream into memory in order to generate the etag based on the whole content
                inputStream = resultStream.readBytes().inputStream()
                resultStream.close() //close original stream, it was already copied to byte array
                serverEtag = Util.getChecksumAndReset(inputStream)
            }
            response.setHeader(ETAG, serverEtag)
            if (serverEtag == ctx.req.getHeader(IF_NONE_MATCH)) {
                response.status = 304
                inputStream.close()
                return // don't write body
            }
        }
        inputStream.copyTo(outputStreamWrapper)
        inputStream.close()
        outputStreamWrapper.finalizeCompression()
    }

}

private const val BR = "br"
private const val GZIP = "gzip"

class OutputStreamWrapper(private val ctx: Context, config: JavalinConfig) : ServletOutputStream() {

    private val acceptsBrotli by lazy { ctx.req.getHeader(ACCEPT_ENCODING)?.contains(BR, ignoreCase = true) == true }
    private val acceptsGzip by lazy { ctx.req.getHeader(ACCEPT_ENCODING)?.contains(GZIP, ignoreCase = true) == true }
    private val compression = config.inner.compressionStrategy
    private val response: HttpServletResponse by lazy { ctx.res }

    private var compressedStream: OutputStream? = null

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (compressedStream == null && isCompressible(length)) {
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

    private fun isCompressible(length: Int) =
        length >= minSizeForCompression && !excludedMimeType(response.contentType) && response.getHeader(CONTENT_ENCODING).isNullOrEmpty()

    private fun excludedMimeType(mimeType: String?) =
        if (mimeType.isNullOrBlank()) false else excludedMimeTypes.any { mimeType.contains(it, ignoreCase = true) }

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
