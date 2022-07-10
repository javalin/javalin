package io.javalin.config

import io.javalin.compression.CompressionStrategy
import io.javalin.compression.Gzip

class CompressionConfig(private val pvt: PrivateConfig) {

    fun custom(compressionStrategy: CompressionStrategy) {
        pvt.compressionStrategy = compressionStrategy
    }

    fun brotliAndGzip(gzipLevel: Int = 6, brotliLevel: Int = 4) {
        pvt.compressionStrategy = CompressionStrategy(io.javalin.compression.Brotli(brotliLevel), Gzip(gzipLevel))
    }

    fun gzipOnly(level: Int = 6) {
        pvt.compressionStrategy = CompressionStrategy(null, Gzip(level))
    }

    fun brotliOnly(level: Int = 4) {
        pvt.compressionStrategy = CompressionStrategy(io.javalin.compression.Brotli(level), null)
    }

    fun none() {
        pvt.compressionStrategy = CompressionStrategy(null, null)
    }
}
