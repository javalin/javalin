package io.javalin.core.config

import io.javalin.core.compression.Brotli
import io.javalin.core.compression.CompressionStrategy
import io.javalin.core.compression.Gzip

class CompressionConfig(private val inner: InnerConfig) {

    fun custom(compressionStrategy: CompressionStrategy) {
        inner.compressionStrategy = compressionStrategy
    }

    fun brotliAndGzip(gzipLevel: Int = 6, brotliLevel: Int = 4) {
        inner.compressionStrategy = CompressionStrategy(Brotli(brotliLevel), Gzip(gzipLevel))
    }

    fun gzipOnly(level: Int = 6) {
        inner.compressionStrategy = CompressionStrategy(null, Gzip(level))
    }

    fun brotliOnly(level: Int = 4) {
        inner.compressionStrategy = CompressionStrategy(Brotli(level), null)
    }

    fun none() = CompressionStrategy(null, null)

}
