package io.javalin.http

import io.javalin.core.JavalinConfig
import io.javalin.core.util.Header
import io.javalin.core.util.Util
import java.io.InputStream
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
    companion object {
        @JvmStatic
        var minSize = 1500 // 1500 is the size of a packet, compressing responses smaller than this serves no purpose
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        when {
            len < minSize ->
                super.write(b, off, len) // no compression
            rwc.accepts.contains("br", ignoreCase = true) && rwc.compressionStrategy.brotli != null -> {
                if (!res.containsHeader(Header.CONTENT_ENCODING)) {
                    res.setHeader(Header.CONTENT_ENCODING, "br")
                }
                rwc.config.inner.compressionStrategy.brotli?.write(res.outputStream, b)
            }
            rwc.accepts.contains("gzip", ignoreCase = true) && rwc.compressionStrategy.gzip != null -> {
                if (!res.containsHeader(Header.CONTENT_ENCODING)) {
                    res.setHeader(Header.CONTENT_ENCODING, "gzip")
                    rwc.config.inner.compressionStrategy.gzip?.create(res.outputStream) //First write, so create new gzip stream
                }
                rwc.config.inner.compressionStrategy.gzip?.write(b, off, len)
            }
            else ->
                super.write(b, off, len)
        }
    }

    fun finalize() {
        val enc = res.getHeader(Header.CONTENT_ENCODING) ?: ""
        if (enc.contains("gzip", ignoreCase = true)) {
            rwc.config.inner.compressionStrategy.gzip?.finish()
        }
        /*
        else if (enc.contains("br", ignoreCase = true)) {
            rwc.config.inner.compressionStrategy.brotli?.finish(res.outputStream)
        }
        */
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

    override fun isReady(): Boolean = res.outputStream.isReady
    override fun setWriteListener(writeListener: WriteListener?) = res.outputStream.setWriteListener(writeListener)
    override fun write(b: Int) {
        res.outputStream.write(b)
    }
}
