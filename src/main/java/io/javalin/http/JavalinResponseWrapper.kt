package io.javalin.http

import io.javalin.core.JavalinConfig
import io.javalin.core.util.Header
import io.javalin.core.util.Util
import org.meteogroup.jbrotli.Brotli
import org.meteogroup.jbrotli.io.BrotliOutputStream
import java.io.InputStream
import java.io.OutputStream
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

    companion object {
        @JvmStatic
        var minSizeForCompression = 1500 // 1500 is the size of a packet, compressing responses smaller than this serves no purpose
        @JvmStatic
        var maxBufferSize = 1000000 // Size limit in bytes, after which the stream buffer is flushed and any further writing is streamed
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        when {
            isFirstWrite && len < minSizeForCompression -> super.write(b, off, len) // no compression
            rwc.accepts.contains("br", ignoreCase = true) && rwc.compressionStrategy.brotli != null -> {
                if (res.getHeader(Header.CONTENT_ENCODING) != "br") {
                    res.setHeader(Header.CONTENT_ENCODING, "br")
                    compressorOutputStream = BrotliOutputStream(res.outputStream, rwc.compressionStrategy.brotli.brotliParameter)
                }
                (compressorOutputStream as BrotliOutputStream).write(b, off, len)
            }
            rwc.accepts.contains("gzip", ignoreCase = true) && rwc.compressionStrategy.gzip != null -> {
                if (res.getHeader(Header.CONTENT_ENCODING) != "gzip") {
                    res.setHeader(Header.CONTENT_ENCODING, "gzip")
                    compressorOutputStream = LeveledGzipStream(res.outputStream, rwc.compressionStrategy.gzip.level)
                }
                (compressorOutputStream as LeveledGzipStream).write(b, off, len)
            }
            else -> super.write(b, off, len) // no compression
        }
        isFirstWrite = false
    }

    fun finalize() {
        if (res.getHeader(Header.CONTENT_ENCODING) == "gzip") {
            (compressorOutputStream as LeveledGzipStream).finish()
        }
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
