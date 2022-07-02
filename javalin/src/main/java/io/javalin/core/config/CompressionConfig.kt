package io.javalin.core.config

import io.javalin.core.compression.Brotli
import io.javalin.core.compression.CompressionStrategy
import io.javalin.core.compression.Gzip

class CompressionConfig(private val pvt: PrivateConfig) {

    fun custom(compressionStrategy: CompressionStrategy) {
        pvt.compressionStrategy = compressionStrategy
    }

    fun brotliAndGzip(gzipLevel: Int = 6, brotliLevel: Int = 4) {
        pvt.compressionStrategy = CompressionStrategy(Brotli(brotliLevel), Gzip(gzipLevel))
    }

    fun gzipOnly(level: Int = 6) {
        pvt.compressionStrategy = CompressionStrategy(null, Gzip(level))
    }

    fun brotliOnly(level: Int = 4) {
        pvt.compressionStrategy = CompressionStrategy(Brotli(level), null)
    }

    fun none() {
        pvt.compressionStrategy = CompressionStrategy(null, null)
    }
}
