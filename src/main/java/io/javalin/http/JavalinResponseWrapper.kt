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

class JavalinResponseWrapper(res: HttpServletResponse, rwc: ResponseWrapperContext) : HttpServletResponseWrapper(res) {
    private val outputStreamWrapper by lazy { OutputStreamWrapper(res, rwc) }
    override fun getOutputStream() = outputStreamWrapper
    fun write(resultStream: InputStream?) = if (resultStream != null) outputStreamWrapper.write(resultStream) else Unit
}

class ResponseWrapperContext(request: HttpServletRequest, val config: JavalinConfig) {
    val accepts = request.getHeader(Header.ACCEPT_ENCODING) ?: ""
    val clientEtag = request.getHeader(Header.IF_NONE_MATCH) ?: ""
    val type = HandlerType.fromServletRequest(request)
    val autogenerateEtags = config.autogenerateEtags
    val compressionStrategy = config.inner.compressionStrategy
}

class OutputStreamWrapper(val res: HttpServletResponse, val rwc: ResponseWrapperContext) : ServletOutputStream() {

    val streamBuffer = ByteArrayOutputStream()
    var sizeLimitExceeded = false

    var gzipCompressor: MyGzipOutputStream? = null

    companion object {
        @JvmStatic
        var minSize = 1500 // 1500 is the size of a packet, compressing responses smaller than this serves no purpose
        @JvmStatic
        var sizeLimit = 1000000 // Size limit in bytes, after which the stream buffer is flushed and any further writing is streamed
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        if(!sizeLimitExceeded) { // size limit not exceeded, so we write all output to the stream buffer
            streamBuffer.write(b, off, len)
            if(streamBuffer.size() > sizeLimit) {
                sizeLimitExceeded = true // Size limit has just been exceeded, future writes will be handled differently
                streamToOutput(streamBuffer.toByteArray(), 0, streamBuffer.size()) // flush buffer to output
                streamBuffer.reset()
            }
        } else { // size limit has already been exceeded. Means this is a big piece of content, so we switch to streaming
            streamToOutput(b, off, len)
        }
    }

    fun finalize() {
        if(!sizeLimitExceeded) { // Write buffer to output stream
            writeToOutput()
        }

        val enc = res.getHeader(Header.CONTENT_ENCODING) ?: ""
        if (enc.contains("gzip", ignoreCase = true)) {
            //rwc.config.inner.compressionStrategy.gzip?.finish() // Finalize gzip stream. This must be done with either write method
            gzipCompressor?.finish()
        }
    }

    fun write(resultStream: InputStream) {
        if (res.getHeader(Header.ETAG) != null || (rwc.autogenerateEtags && rwc.type == HandlerType.GET)) {
            val serverEtag = res.getHeader(Header.ETAG) ?: Util.getChecksumAndReset(resultStream) // calculate if not set
            res.setHeader(Header.ETAG, serverEtag)
            if (serverEtag == rwc.clientEtag) {
                res.status = 304
                return // don't write body
            }
        }
        write(resultStream.readBytes())
        resultStream.close()
        finalize()
    }

    private fun streamToOutput(b: ByteArray, off: Int, len: Int) {
        when {
            // Gzip only for now while streaming, as Brotli stream compression currently doesn't work
            rwc.accepts.contains("gzip", ignoreCase = true) && rwc.compressionStrategy.gzip != null -> {
                if (!res.containsHeader(Header.CONTENT_ENCODING)) {
                    res.setHeader(Header.CONTENT_ENCODING, "gzip")
                    if(gzipCompressor == null) {
                        gzipCompressor = MyGzipOutputStream(res.outputStream, rwc.compressionStrategy.gzip?.level)
                    }
                }
                gzipCompressor?.write(b, off, len)
            }
            else -> super.write(b, off, len) // no compression
        }
    }

    private fun writeToOutput() {
        when {
            streamBuffer.size() < minSize -> super.write(streamBuffer.toByteArray(), 0, streamBuffer.size()) // data set too small
            rwc.accepts.contains("br", ignoreCase = true) && rwc.compressionStrategy.brotli != null -> {
                if (!res.containsHeader(Header.CONTENT_ENCODING)) {
                    res.setHeader(Header.CONTENT_ENCODING, "br")
                }
                rwc.config.inner.compressionStrategy.brotli?.write(res.outputStream, streamBuffer)
            }
            rwc.accepts.contains("gzip", ignoreCase = true) && rwc.compressionStrategy.gzip != null -> {
                if (!res.containsHeader(Header.CONTENT_ENCODING)) {
                    res.setHeader(Header.CONTENT_ENCODING, "gzip")
                    if(gzipCompressor == null) {
                        gzipCompressor = MyGzipOutputStream(res.outputStream, rwc.compressionStrategy.gzip?.level)
                    }
                }
                gzipCompressor?.write(streamBuffer)
            }
            else -> super.write(streamBuffer.toByteArray(), 0, streamBuffer.size()) // no compression
        }
    }

    override fun isReady(): Boolean = res.outputStream.isReady
    override fun setWriteListener(writeListener: WriteListener?) = res.outputStream.setWriteListener(writeListener)
    override fun write(b: Int) {
        res.outputStream.write(b)
    }
}

class MyGzipOutputStream(out: OutputStream, level: Int) : GZIPOutputStream(out) {
    init {
        this.def.setLevel(level)
    }

    fun write(data: ByteArrayOutputStream) {
        super.write(data.toByteArray(), 0, data.size())
    }
}
