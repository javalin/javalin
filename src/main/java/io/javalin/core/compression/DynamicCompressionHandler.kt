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
            brotliShouldBeDone(ctx) -> { //Do Brotli
                val level = compressionStrategy.brotliLevel
                BrotliWrapper(level).use { brWrapper ->
                    res.setHeader(Header.CONTENT_ENCODING, "br")
                    res.outputStream.write(brWrapper.compressByteArray(resultStream.readBytes()))
                }
                return
            }
            gzipShouldBeDone(ctx) -> { //Do GZIP
                val level = compressionStrategy.gzipLevel
                GzipWrapper(res.outputStream, true, level).use { gzippedStream ->
                    res.setHeader(Header.CONTENT_ENCODING, "gzip")
                    resultStream.copyTo(gzippedStream)
                }
                return
            }
            else -> {
                resultStream.copyTo(res.outputStream) //No compression
            }
        }
    }

    //PRIVATE methods
    private fun resultExceedsMtu(ctx: Context): Boolean {
        return ctx.resultStream()?.available() ?: 0 > 1500 // mtu is apparently ~1500 bytes
    }

    private fun supportsEncoding(ctx: Context, encoding: String): Boolean {
        return (ctx.header(Header.ACCEPT_ENCODING) ?: "").contains(encoding, ignoreCase = true)
    }

    private fun gzipShouldBeDone(ctx: Context): Boolean {
        return  compressionStrategy.gzipEnabled
                && resultExceedsMtu(ctx)
                && supportsEncoding(ctx, "gzip")
    }

    private fun brotliShouldBeDone(ctx: Context): Boolean {
        return  compressionStrategy.brotliEnabled
                && resultExceedsMtu(ctx)
                && supportsEncoding(ctx, "br")
    }
}
