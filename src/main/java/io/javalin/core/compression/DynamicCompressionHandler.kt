package io.javalin.core.compression

import io.javalin.core.JavalinConfig
import io.javalin.core.util.Header
import io.javalin.http.Context
import javax.servlet.http.HttpServletResponse

/**
 * Class designed to handle dynamic content compression for Javalin.
 *
 * Uses DynamicCompressionStrategy within JavalinConfig to determine the type and level of compression to be applied.
 *
 * @see DynamicCompressionStrategy
 */
class DynamicCompressionHandler(val ctx: Context, val config: JavalinConfig) {

    private val compressionStrategy = config.inner.dynamicCompressionStrategy

    /**
     * @param res The response that we wish to encode
     */
    fun compressResponse(res: HttpServletResponse) {
        val resultStream = ctx.resultStream()!!
        when {
            brotliShouldBeDone() -> { //Do Brotli
                res.setHeader(Header.CONTENT_ENCODING, "br")
                compressionStrategy.brotli?.write(res.outputStream, resultStream.readBytes())
                return
            }
            gzipShouldBeDone() -> { //Do GZIP
                res.setHeader(Header.CONTENT_ENCODING, "gzip")
                compressionStrategy.gzip?.write(res.outputStream, resultStream.readBytes())
                return
            }
            else -> {
                resultStream.copyTo(res.outputStream) //No compression
            }
        }
    }

    private fun resultExceedsMtu(): Boolean =
            ctx.resultStream()?.available() ?: 0 > 1500 // mtu is apparently ~1500 bytes

    private fun supportsEncoding(encoding: String): Boolean =
            (ctx.header(Header.ACCEPT_ENCODING) ?: "").contains(encoding, ignoreCase = true)

    private fun gzipShouldBeDone(): Boolean =
            compressionStrategy.gzip != null && resultExceedsMtu() && supportsEncoding("gzip")

    private fun brotliShouldBeDone(): Boolean =
            compressionStrategy.brotli != null && resultExceedsMtu() && supportsEncoding("br")

}
