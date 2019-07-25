package io.javalin.core.compression

import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import org.meteogroup.jbrotli.Brotli
import org.meteogroup.jbrotli.BrotliCompressor
import java.io.ByteArrayOutputStream

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
        Util.ensureDependencyPresent(OptionalDependency.JBROTLI)
        brotliCompressor = BrotliCompressor()
        brotliParameter = Brotli.Parameter(Brotli.DEFAULT_MODE, level, Brotli.DEFAULT_LGWIN, Brotli.DEFAULT_LGBLOCK)
    }

    /**
     * @param out The target output stream
     * @param data data to compress
     */
    fun write(out: OutputStream, data: ByteArray, off: Int, len: Int) {
        //Needed because compressing small data sets sometimes yields a bigger output than the original
        val size = if (len >= 8192) len else 8192
        val output = ByteArray(size)
        val compressedLength = brotliCompressor.compress(brotliParameter, data, off, len, output, 0, size)
        out.write(output.copyOfRange(0, compressedLength))
    }

    fun write(out: OutputStream, data: ByteArrayOutputStream) {
        write(out, data.toByteArray(), 0, data.size())
    }
}
