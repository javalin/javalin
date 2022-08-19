package io.javalin.config

import io.javalin.compression.Brotli
import io.javalin.compression.CompressionStrategy
import io.javalin.compression.Gzip

class CompressionConfig(private val pvt: PrivateConfig) {

    fun custom(compressionStrategy: CompressionStrategy) {
        pvt.compressionStrategy = compressionStrategy
    }

    @JvmOverloads
    fun brotliAndGzip(gzipLevel: Int = 6, brotliLevel: Int = 4) {
        pvt.compressionStrategy = CompressionStrategy(Brotli(brotliLevel), Gzip(gzipLevel))
    }

    @JvmOverloads
    fun gzipOnly(level: Int = 6) {
        pvt.compressionStrategy = CompressionStrategy(null, Gzip(level))
    }

    @JvmOverloads
    fun brotliOnly(level: Int = 4) {
        pvt.compressionStrategy = CompressionStrategy(Brotli(level), null)
    }

    fun none() {
        pvt.compressionStrategy = CompressionStrategy(null, null)
    }
}
