package io.javalin.core.compression

import org.meteogroup.jbrotli.Brotli
import org.meteogroup.jbrotli.BrotliCompressor

import java.io.OutputStream

/**
 * Kotlin wrapper for jbrotli library.
 *
 * @param level Compression level. Higher yields better (but slower) compression. Range 0..11, default = 4
 */
@Deprecated("WARNING: Brotli compression is an experimental feature!")
class Brotli(val level: Int = 4) {

    private val brotliCompressor: BrotliCompressor
    private val brotliParameter: Brotli.Parameter

    init {
        require(level in 0..11) { "Valid range for parameter level is 0 to 11" }
        brotliCompressor = BrotliCompressor()
        brotliParameter = Brotli.Parameter(Brotli.DEFAULT_MODE, level, Brotli.DEFAULT_LGWIN, Brotli.DEFAULT_LGBLOCK)
    }

    /**
     * @param out The target output stream
     * @param data data to compress
     */
    fun write(out: OutputStream, data: ByteArray) {
        val size = if (data.size >= 8192) data.size else 8192
        val output = ByteArray(size)
        val compressedLength = brotliCompressor.compress(brotliParameter, data, output)
        out.write(output.copyOfRange(0, compressedLength))
    }
}
