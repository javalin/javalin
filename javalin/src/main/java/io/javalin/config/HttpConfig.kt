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
    @JvmField var maxRequestSize = 1_000_000L // increase this or use inputstream to handle large requests
    @JvmField var defaultContentType = ContentType.PLAIN
    @JvmField var asyncTimeout = 0L
    //@formatter:on

    /**
     * Sets a custom CompressionStrategy.
     * @param compressionStrategy the strategy to use
     */
    fun customCompression(compressionStrategy: CompressionStrategy) {
        cfg.pvt.compressionStrategy = compressionStrategy
    }

    /**
     * Sets a CompressionStrategy using both gzip and brotli.
     * @param gzipLevel the gzip compression level
     * @param brotliLevel the brotli compression level
     */
    @JvmOverloads
    fun brotliAndGzipCompression(gzipLevel: Int = 6, brotliLevel: Int = 4) {
        cfg.pvt.compressionStrategy = CompressionStrategy(Brotli(brotliLevel), Gzip(gzipLevel))
    }

    /**
     * Sets a CompressionStrategy using gzip
     * @param level the gzip compression level
     */
    @JvmOverloads
    fun gzipOnlyCompression(level: Int = 6) {
        cfg.pvt.compressionStrategy = CompressionStrategy(null, Gzip(level))
    }


    /**
     * Sets a CompressionStrategy using brotli.
     * @param level the brotli compression level
     */
    @JvmOverloads
    fun brotliOnlyCompression(level: Int = 4) {
        cfg.pvt.compressionStrategy = CompressionStrategy(Brotli(level), null)
    }

    /**
     * Disable Compression
     */
    fun disableCompression() {
        cfg.pvt.compressionStrategy = CompressionStrategy(null, null)
    }
}
