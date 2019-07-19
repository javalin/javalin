package io.javalin.http

import io.javalin.core.compression.CompressionStrategy
import io.javalin.core.util.Header
import javax.servlet.ServletOutputStream
import javax.servlet.WriteListener
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletResponseWrapper

/**
 * This class acts as an "interceptor" for handling both static and dynamic responses.
 * We use it to wrap the "res" parameter inside JavalinServlet#service
 *
 * For dynamic responses, we then simply call res.outputStream.write() inside service#writeResult and feed it the resultStream
 *
 * For static responses, we intercept Jetty's writing by feeding its ResourceHandler the wrapped "res" param instead of the original.
 * Jetty uses the same outputStream.write() method to write static content to output after handling it.
 * Because of this, we are able to capture Jetty's output in here, modify it as needed, then commit it ourselves
 *
 * For the moment, this is being used to bypass Jetty's compression handling, so we can use our own (which supports Brotli and dynamic content),
 * however, there could be more uses for this in the future.
 */
class ResponseHandler(
        response: HttpServletResponse,
        ctx: Context,
        compressionStrategy: CompressionStrategy) : HttpServletResponseWrapper(response)
{
    private val outputWrapper = OutputWrapper(response, ctx, compressionStrategy)
    override fun getOutputStream() = outputWrapper

    class OutputWrapper(
            private val res: HttpServletResponse,
            private val ctx: Context,
            private val compressionStrategy: CompressionStrategy) : ServletOutputStream()
    {
        override fun isReady(): Boolean = res.outputStream.isReady
        override fun setWriteListener(writeListener: WriteListener?) = res.outputStream.setWriteListener(writeListener)
        override fun write(b: Int) {
            res.outputStream.write(b)
        }
        override fun write(b: ByteArray, off: Int, len: Int) {
            when {
                brotliShouldBeDone(len) -> {
                    res.setHeader(Header.CONTENT_ENCODING, "br")
                    compressionStrategy.brotli?.write(res.outputStream, b)
                }
                gzipShouldBeDone(len) -> {
                    res.setHeader(Header.CONTENT_ENCODING, "gzip")
                    compressionStrategy.gzip?.write(res.outputStream, b)
                }
                else -> {
                    super.write(b, off, len)
                }
            }
        }

        private fun resultExceedsMtu(length: Int): Boolean =
                ctx.resultStream()?.available() ?: 0 > 1500 || length > 1500 // mtu is apparently ~1500 bytes

        private fun supportsEncoding(encoding: String): Boolean =
                (ctx.header(Header.ACCEPT_ENCODING) ?: "").contains(encoding, ignoreCase = true)

        private fun gzipShouldBeDone(length: Int): Boolean =
                compressionStrategy.gzip != null && resultExceedsMtu(length) && supportsEncoding("gzip")

        private fun brotliShouldBeDone(length: Int): Boolean =
                compressionStrategy.brotli != null && resultExceedsMtu(length) && supportsEncoding("br")
    }
}
