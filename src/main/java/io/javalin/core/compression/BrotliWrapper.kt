package io.javalin.core.compression

import org.meteogroup.jbrotli.Brotli
import org.meteogroup.jbrotli.BrotliCompressor

import java.io.Closeable

/**
 * Kotlin wrapper for jbrotli library.
 * DynamicCompressionStrategy uses this to perform Brotli compression
 * @see DynamicCompressionStrategy
 *
 * @param level Compression level. Range 0..11, default = 4
 */
class BrotliWrapper(val level: Int = 4): Closeable {

    private val brotliCompressor: BrotliCompressor
    private val brotliParameter: Brotli.Parameter

    init {
        require(level in 0..11) {
            "Valid range for parameter level is 0 to 11"
        }
        brotliCompressor = BrotliCompressor()
        brotliParameter = Brotli.Parameter(Brotli.DEFAULT_MODE, level, Brotli.DEFAULT_LGWIN, Brotli.DEFAULT_LGBLOCK)
    }

    /**
     * @param input Byte array to compress
     * @return Compressed byte array
     */
    fun compressByteArray(input: ByteArray): ByteArray {
        val output = ByteArray(input.size)
        val compressedLength = brotliCompressor.compress(brotliParameter, input, output)
        return output.copyOfRange(0, compressedLength)
    }

    //not used, but needs to be overriden because we implement Closeable
    override fun close() {}
}
