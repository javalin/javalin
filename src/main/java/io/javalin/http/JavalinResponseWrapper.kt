package io.javalin.http

import io.javalin.core.JavalinConfig
import io.javalin.core.util.Header
import io.javalin.core.util.Util
import java.io.ByteArrayOutputStream
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

    private val buffer = ByteArrayOutputStream()
    private lateinit var compressorOutputStream: OutputStream

    private var isFirstWrite = true
    private var brotliEnabled = false
    private var gzipEnabled = false

    companion object {
        @JvmStatic
        var minSizeForCompression = 1500 // 1500 is the size of a packet, compressing responses smaller than this serves no purpose
        @JvmStatic
        var maxBufferSize = 1000000 // Size limit in bytes, after which the stream buffer is flushed and any further writing is streamed
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

    /**
     * Buffering is a temporary workaround for brotli, because jbrotli stream compression is currently broken.
     * The idea is we buffer the response (up to 1 MB max by default), then compress it and send it to output.
     * If the total content size exceeds this limit, we switch to streaming and fail over to gzip (if enabled).
     *
     * This should allow brotli to work for most responses, as not many will exceed 1 MB in size.
     * Code will be refactored to full streaming once the brotli issue is fixed.
     *
     * Buffering only happens if brotli is enabled in compression strategy. Gzip (or uncompressed) go directly to streaming.
     */
    override fun write(b: ByteArray, off: Int, len: Int) {
        if (isFirstWrite) { // first write
            setAvailableCompressors(len)
            isFirstWrite = false
        }
        if (buffer.size() <= maxBufferSize && brotliEnabled) { // size limit not exceeded, so we write all output to the stream buffer
            buffer.write(b, off, len)
            if (buffer.size() > maxBufferSize) {
                // Size limit has just been exceeded, flush the buffer and switch to streaming
                streamToOutput(buffer.toByteArray(), 0, buffer.size())
            }
        } else {
            streamToOutput(b, off, len)
        }
    }

    private fun streamToOutput(b: ByteArray, off: Int, len: Int) {
        when {
            gzipEnabled -> { // Gzip only for now while streaming, as Brotli stream compression currently doesn't work
                if (res.getHeader(Header.CONTENT_ENCODING) != "gzip") {
                    res.setHeader(Header.CONTENT_ENCODING, "gzip")
                    compressorOutputStream = LeveledGzipStream(res.outputStream, rwc.compressionStrategy.gzip?.level ?: -1)
                }
                (compressorOutputStream as LeveledGzipStream).write(b, off, len)
            }
            else -> super.write(b, off, len) // no compression
        }
    }

    fun finalize() {
        if (buffer.size() < maxBufferSize && brotliEnabled) { // compress and output the buffer
            writeBrotliToOutput()
            return
        }

        // did we gzip? If so, finalize the gzip stream
        if (res.getHeader(Header.CONTENT_ENCODING) == "gzip") {
            (compressorOutputStream as LeveledGzipStream).finish()
        }
    }

    private fun writeBrotliToOutput() {
        rwc.config.inner.compressionStrategy.brotli?.write(res, buffer)
    }

    private fun setAvailableCompressors(len: Int) {
        // enable compression based on length of first write and mime type
        if (len >= minSizeForCompression && isCompressableMimeType(res.contentType)) {
            brotliEnabled = rwc.accepts.contains("br", ignoreCase = true) && rwc.compressionStrategy.brotli != null
            gzipEnabled = rwc.accepts.contains("gzip", ignoreCase = true) && rwc.compressionStrategy.gzip != null
        }
    }

    private fun isCompressableMimeType(mimeType: String?): Boolean {
        for(excludedMimeType in excludedMimeTypes) {
            val mimeType = mimeType ?: "" // We compress content with null mime type. Jetty does the same
            if(mimeType.contains(excludedMimeType)) {
                return false
            }
        }
        return true
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
