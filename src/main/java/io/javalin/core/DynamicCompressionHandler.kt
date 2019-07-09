package io.javalin.core

import io.javalin.core.util.Header
import io.javalin.http.Context
import javax.servlet.http.HttpServletResponse

class DynamicCompressionHandler(val ctx: Context, val config: JavalinConfig) {

    val compressionStrategy = config.inner.dynamicCompressionStrategy

    fun compressResponse(res: HttpServletResponse) {
        val resultStream = ctx.resultStream()!!
        if (brotliShouldBeDone(ctx)) {
            res.setHeader(Header.CONTENT_ENCODING, "br")
            val level = compressionStrategy?.brotliLevel ?: DynamicCompressionStrategy.BROTLI_DEFAULT_LEVEL
            val brotliWrapper = BrotliWrapper(level)
            res.outputStream.write(brotliWrapper.compressBytes(resultStream.readBytes()))
            return
        }
        else if (gzipShouldBeDone(ctx)) {
            val level = compressionStrategy?.gzipLevel ?: DynamicCompressionStrategy.GZIP_DEFAULT_LEVEL
            GZIPWrapper(res.outputStream, true).setLevel(level).use { gzippedStream ->
                res.setHeader(Header.CONTENT_ENCODING, "gzip")
                resultStream.copyTo(gzippedStream)
            }
            return
        }
        resultStream.copyTo(res.outputStream) // no compression
    }

    private fun resultExceedsMTU(ctx: Context): Boolean {
        return ctx.resultStream()?.available() ?: 0 > 1500 // mtu is apparently ~1500 bytes
    }

    private fun supportsEncoding(ctx: Context, encoding: String): Boolean {
        return (ctx.header(Header.ACCEPT_ENCODING) ?: "").contains(encoding, ignoreCase = true)
    }

    private fun gzipShouldBeDone(ctx: Context): Boolean {
        //If compressionStrategy is null, we check the old dynamicGzip boolean. This is important for backwards compatibility
        return compressionStrategy?.isGzipEnabled ?: config.dynamicGzip
                && resultExceedsMTU(ctx)
                && supportsEncoding(ctx, "gzip")
    }

    private fun brotliShouldBeDone(ctx: Context): Boolean {
        return compressionStrategy?.isBrotliEnabled ?: false
                && resultExceedsMTU(ctx)
                && supportsEncoding(ctx, "br")
    }
}
