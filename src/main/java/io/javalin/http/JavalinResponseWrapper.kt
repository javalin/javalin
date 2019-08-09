package io.javalin.http

import com.nixxcode.jvmbrotli.enc.BrotliOutputStream
import com.nixxcode.jvmbrotli.enc.Encoder
import io.javalin.core.JavalinConfig
import io.javalin.core.util.Header
import io.javalin.core.util.Util
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.logging.Level
import java.util.zip.GZIPOutputStream
import javax.servlet.ServletOutputStream
import javax.servlet.WriteListener
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

class ResponseWrapperContext(request: HttpServletRequest, val config: JavalinConfig) {
    val accepts = request.getHeader(Header.ACCEPT_ENCODING) ?: ""
    val clientEtag = request.getHeader(Header.IF_NONE_MATCH) ?: ""
    val type = HandlerType.fromServletRequest(request)
    val compressionStrategy = config.inner.compressionStrategy
}

class JavalinResponseWrapper(val res: HttpServletResponse, val rwc: ResponseWrapperContext) : HttpServletResponseWrapper(res) {

    private val outputStreamWrapper by lazy { OutputStreamWrapper(res, rwc) }
    override fun getOutputStream() = outputStreamWrapper

    fun write(resultStream: InputStream?) {
        if (resultStream == null) return
        if (res.getHeader(Header.ETAG) != null || (rwc.config.autogenerateEtags && rwc.type == HandlerType.GET)) {
            val serverEtag = res.getHeader(Header.ETAG) ?: Util.getChecksumAndReset(resultStream) // calculate if not set
            res.setHeader(Header.ETAG, serverEtag)
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

class OutputStreamWrapper(val res: HttpServletResponse, val rwc: ResponseWrapperContext) : ServletOutputStream() {

    private lateinit var compressorOutputStream: OutputStream

    private var isFirstWrite = true
    private var brotliEnabled = false
    private var gzipEnabled = false

    companion object {
        @JvmStatic
        var minSizeForCompression = 1500 // 1500 is the size of a packet, compressing responses smaller than this serves no purpose
        @JvmStatic
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
        if (isFirstWrite) { // first write
            setAvailableCompressors(len)
            isFirstWrite = false
        }

        when {
            brotliEnabled -> {
                if (res.getHeader(Header.CONTENT_ENCODING) != "br") {
                    res.setHeader(Header.CONTENT_ENCODING, "br")
                    compressorOutputStream = LeveledBrotliStream(res.outputStream, rwc.compressionStrategy.brotli!!.level)
                }
                (compressorOutputStream as LeveledBrotliStream).write(b, off, len)
            }
            gzipEnabled -> {
                if (res.getHeader(Header.CONTENT_ENCODING) != "gzip") {
                    res.setHeader(Header.CONTENT_ENCODING, "gzip")
                    compressorOutputStream = LeveledGzipStream(res.outputStream, rwc.compressionStrategy.gzip!!.level)
                }
                (compressorOutputStream as LeveledGzipStream).write(b, off, len)
            }
            else -> super.write(b, off, len) // no compression
        }
    }

    // If we used compression, finalize the stream
    fun finalize() {
        if (res.getHeader(Header.CONTENT_ENCODING) == "br" && brotliEnabled) {
            (compressorOutputStream as BrotliOutputStream).close()
        }
        if (res.getHeader(Header.CONTENT_ENCODING) == "gzip" && gzipEnabled) {
            (compressorOutputStream as LeveledGzipStream).finish()
        }
    }

    private fun setAvailableCompressors(len: Int) {
        // enable compression based on length of first write and mime type
        if (len >= minSizeForCompression && !excludedMimeType(res.contentType ?: "") && res.getHeader(Header.CONTENT_ENCODING).isNullOrEmpty()) {
            brotliEnabled = rwc.accepts.contains("br", ignoreCase = true) && rwc.compressionStrategy.brotli != null
            if (brotliEnabled) return
            gzipEnabled = rwc.accepts.contains("gzip", ignoreCase = true) && rwc.compressionStrategy.gzip != null
        }
    }

    private fun excludedMimeType(mimeType: String): Boolean {
        if (mimeType.isEmpty()) {
            return false
        }
        return excludedMimeTypes.any { excluded -> mimeType.contains(excluded, ignoreCase = true) }
    }

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
