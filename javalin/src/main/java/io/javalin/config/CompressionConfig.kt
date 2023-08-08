package io.javalin.config

import io.javalin.compression.Brotli
import io.javalin.compression.CompressionStrategy
import io.javalin.compression.Gzip

class CompressionConfig(private val cfg: JavalinConfig) {

    fun custom(compressionStrategy: CompressionStrategy) {
        cfg.pvt.compressionStrategy = compressionStrategy
    }

    @JvmOverloads
    fun brotliAndGzip(gzipLevel: Int = 6, brotliLevel: Int = 4) {
        cfg.pvt.compressionStrategy = CompressionStrategy(Brotli(brotliLevel), Gzip(gzipLevel))
    }

    @JvmOverloads
    fun gzipOnly(level: Int = 6) {
        cfg.pvt.compressionStrategy = CompressionStrategy(null, Gzip(level))
    }

    @JvmOverloads
    fun brotliOnly(level: Int = 4) {
        cfg.pvt.compressionStrategy = CompressionStrategy(Brotli(level), null)
    }

    fun none() {
        cfg.pvt.compressionStrategy = CompressionStrategy(null, null)
    }

}
