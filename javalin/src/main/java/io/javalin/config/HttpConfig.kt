package io.javalin.config

import io.javalin.compression.Brotli
import io.javalin.compression.CompressionStrategy
import io.javalin.compression.Gzip
import io.javalin.http.ContentType

/**
 * Configuration for the HTTP layer.
 *
 * @param cfg the parent Javalin Configuration
 * @see [JavalinConfig.http]
 */
class HttpConfig(private val cfg: JavalinConfig) {
    //@formatter:off
    @JvmField var generateEtags = false
    @JvmField var prefer405over404 = false
    @JvmField var strictFormContentTypes = false
    @JvmField var maxRequestSize = 1_000_000L // increase this or use inputstream to handle large requests
    @JvmField var defaultContentType = ContentType.PLAIN
    @JvmField var asyncTimeout = 0L
    //@formatter:on

    /** Sets a custom CompressionStrategy. */
    fun customCompression(compressionStrategy: CompressionStrategy) {
        cfg.pvt.compressionStrategy = compressionStrategy
    }

    /** Sets a CompressionStrategy using both gzip (default level: 6) and brotli (default level: 4).*/
    @JvmOverloads
    fun brotliAndGzipCompression(gzipLevel: Int = 6, brotliLevel: Int = 4) {
        cfg.pvt.compressionStrategy = CompressionStrategy(Brotli(brotliLevel), Gzip(gzipLevel))
    }

    /** Sets a CompressionStrategy using gzip (default level: 6). */
    @JvmOverloads
    fun gzipOnlyCompression(level: Int = 6) {
        cfg.pvt.compressionStrategy = CompressionStrategy(null, Gzip(level))
    }

    /** Sets a CompressionStrategy using brotli (default level: 4). */
    @JvmOverloads
    fun brotliOnlyCompression(level: Int = 4) {
        cfg.pvt.compressionStrategy = CompressionStrategy(Brotli(level), null)
    }

    /** Disable Compression */
    fun disableCompression() {
        cfg.pvt.compressionStrategy = CompressionStrategy(null, null)
    }
}
