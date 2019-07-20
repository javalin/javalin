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

    class OutputStreamWrapper(val res: HttpServletResponse, val rwc: ResponseWrapperContext) : ServletOutputStream() {

        override fun write(b: ByteArray, off: Int, len: Int) {
            when {
                brotliShouldBeDone(len) -> {
                    res.setHeader(Header.CONTENT_ENCODING, "br")
                    rwc.config.inner.compressionStrategy.brotli?.write(res.outputStream, b)
                }
                gzipShouldBeDone(len) -> {
                    res.setHeader(Header.CONTENT_ENCODING, "gzip")
                    rwc.config.inner.compressionStrategy.gzip?.write(res.outputStream, b)
                }
                else -> super.write(b, off, len)
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
        }

        private fun resultExceedsMtu(length: Int): Boolean = length > 1500 // mtu is apparently ~1500 bytes

        private fun gzipShouldBeDone(length: Int): Boolean =
                rwc.compressionStrategy.gzip != null && resultExceedsMtu(length) && rwc.accepts.contains("gzip", ignoreCase = true)

        private fun brotliShouldBeDone(length: Int): Boolean =
                rwc.compressionStrategy.brotli != null && resultExceedsMtu(length) && rwc.accepts.contains("br", ignoreCase = true)

        override fun isReady(): Boolean = res.outputStream.isReady
        override fun setWriteListener(writeListener: WriteListener?) = res.outputStream.setWriteListener(writeListener)
        override fun write(b: Int) {
            res.outputStream.write(b)
        }
    }
}

class ResponseWrapperContext(request: HttpServletRequest, val config: JavalinConfig) {
    val accepts = request.getHeader(Header.ACCEPT_ENCODING) ?: ""
    val clientEtag = request.getHeader(Header.IF_NONE_MATCH) ?: ""
    val type = HandlerType.fromServletRequest(request)
    val autogenerateEtags = config.autogenerateEtags
    val compressionStrategy = config.inner.compressionStrategy
}
