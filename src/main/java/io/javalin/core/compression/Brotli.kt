package io.javalin.core.compression

import io.javalin.core.util.Header
import io.javalin.core.util.OptionalDependency
import io.javalin.core.util.Util
import org.meteogroup.jbrotli.Brotli
import org.meteogroup.jbrotli.BrotliCompressor
import java.io.ByteArrayOutputStream

import javax.servlet.http.HttpServletResponse

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
    fun write(res: HttpServletResponse, data: ByteArray, off: Int, len: Int) {
        //We need a padded buffer, because compression sometimes yields a bigger output than the original
        val paddedBufferSize = if (len >= 1024) len*2 else 2048
        val output = ByteArray(paddedBufferSize)
        val compressedLength = brotliCompressor.compress(brotliParameter, data, off, len, output, 0, paddedBufferSize)

        if(compressedLength >= len) { //If compressed length is same or bigger, there's no point, let's just write the original
            res.setHeader(Header.CONTENT_ENCODING, "null")
            res.outputStream.write(data, off, len)
        } else {
            res.setHeader(Header.CONTENT_ENCODING, "br")
            res.outputStream.write(output.copyOfRange(0, compressedLength))
        }
    }

    fun write(res: HttpServletResponse, data: ByteArrayOutputStream) {
        write(res, data.toByteArray(), 0, data.size())
    }
}
