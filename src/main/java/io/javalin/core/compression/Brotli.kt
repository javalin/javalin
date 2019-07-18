package io.javalin.core.compression

import org.meteogroup.jbrotli.Brotli
import org.meteogroup.jbrotli.BrotliCompressor
import org.meteogroup.jbrotli.BrotliStreamCompressor

import java.io.OutputStream
import java.nio.ByteBuffer

/**
 * Kotlin wrapper for jbrotli library.
 * DynamicCompressionHandler uses this to perform Brotli compression
 * @see DynamicCompressionHandler
 *
 * @param level Compression level. Higher yields better (but slower) compression. Range 0..11, default = 4
 */
@Deprecated("WARNING: Brotli compression is an experimental feature!")
class Brotli(val level: Int = 4) {

    private val brotliCompressor: BrotliCompressor
    private val brotliParameter: Brotli.Parameter

    init {
        require(level in 0..11) { "Valid range for parameter level is 0 to 11" }
        brotliParameter = Brotli.Parameter(Brotli.DEFAULT_MODE, level, Brotli.DEFAULT_LGWIN, Brotli.DEFAULT_LGBLOCK)
        brotliCompressor = BrotliCompressor()
    }

    /**
     * @param out The target output stream
     * @param data data to compress
     */
    fun write(out: OutputStream, data: ByteArray) {
        out.write(compressArray(data))
    }

    fun compressArray(data: ByteArray): ByteArray {
        val output = ByteArray(data.size)
        val compressedLength = brotliCompressor.compress(brotliParameter, data, output)
        return output.copyOfRange(0, compressedLength)
    }

    //Experimental method for static compression
    fun compressBuffer(data: ByteBuffer): ByteBuffer {
        val bsc = BrotliStreamCompressor(brotliParameter)
        return bsc.compressNext(data, true)
    }
}
