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
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

private const val BR = "br"
private const val GZIP = "gzip"

class ResponseWrapperContext(request: HttpServletRequest, val config: JavalinConfig) {
    val acceptsBrotli by lazy { request.getHeader(ACCEPT_ENCODING)?.contains(BR, ignoreCase = true) == true }
    val acceptsGzip by lazy { request.getHeader(ACCEPT_ENCODING)?.contains(GZIP, ignoreCase = true) == true }
    val clientEtag by lazy { request.getHeader(IF_NONE_MATCH) ?: "" }
    val type by lazy { HandlerType.fromServletRequest(request) }
    val compStrat = config.inner.compressionStrategy
}

class JavalinResponseWrapper(val res: HttpServletResponse, private val rwc: ResponseWrapperContext) : HttpServletResponseWrapper(res) {

    private val outputStreamWrapper by lazy { OutputStreamWrapper(res, rwc) }
    override fun getOutputStream() = outputStreamWrapper

    fun write(resultStream: InputStream?) {
        if (resultStream == null) return
        if (res.getHeader(ETAG) != null || (rwc.config.autogenerateEtags && rwc.type == HandlerType.GET)) {
            val serverEtag = res.getHeader(ETAG) ?: Util.getChecksumAndReset(resultStream) // calculate if not set
            res.setHeader(ETAG, serverEtag)
            if (serverEtag == rwc.clientEtag) {
                res.status = 304
                return // don't write body
            }
        }
        resultStream.copyTo(outputStreamWrapper)
        resultStream.close()
        outputStreamWrapper.finalize()
    }

}

class OutputStreamWrapper(val res: HttpServletResponse, private val rwc: ResponseWrapperContext) : ServletOutputStream() {

    private lateinit var compressingStream: OutputStream

    private var initialized = false
    private var brotliEnabled = false
    private var gzipEnabled = false

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
                "application/x-rar-compressed")
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (!initialized) { // set available compressors, content encoding, and compressing-stream
            val isCompressible = len >= minSizeForCompression && !excludedMimeType(res.contentType ?: "") && res.getHeader(CONTENT_ENCODING).isNullOrEmpty()
            if (isCompressible && rwc.acceptsBrotli && rwc.compStrat.brotli != null) {
                res.setHeader(CONTENT_ENCODING, BR)
                compressingStream = LeveledBrotliStream(res.outputStream, rwc.compStrat.brotli.level)
                brotliEnabled = true
            } else if (isCompressible && rwc.acceptsGzip && rwc.compStrat.gzip != null) {
                res.setHeader(CONTENT_ENCODING, GZIP)
                compressingStream = LeveledGzipStream(res.outputStream, rwc.compStrat.gzip.level)
                gzipEnabled = true
            }
            initialized = true
        }
        when {
            brotliEnabled -> (compressingStream as LeveledBrotliStream).write(b, off, len)
            gzipEnabled -> (compressingStream as LeveledGzipStream).write(b, off, len)
            else -> super.write(b, off, len) // no compression
        }
    }

    fun finalize() {
        when {
            brotliEnabled && res.getHeader(CONTENT_ENCODING) == BR -> (compressingStream as BrotliOutputStream).close()
            gzipEnabled && res.getHeader(CONTENT_ENCODING) == GZIP -> (compressingStream as LeveledGzipStream).finish()
        }
    }

    private fun excludedMimeType(mimeType: String) =
            if (mimeType == "") false else excludedMimeTypes.any { excluded -> mimeType.contains(excluded, ignoreCase = true) }

    override fun isReady(): Boolean = res.outputStream.isReady
    override fun setWriteListener(writeListener: WriteListener?) = res.outputStream.setWriteListener(writeListener)
    override fun write(b: Int) {
        res.outputStream.write(b)
    }
}

class LeveledGzipStream(out: OutputStream, level: Int) : GZIPOutputStream(out) {
    init {
        this.def.setLevel(level)
    }
}

class LeveledBrotliStream(out: OutputStream, level: Int) : BrotliOutputStream(out, Encoder.Parameters().setQuality(level))
