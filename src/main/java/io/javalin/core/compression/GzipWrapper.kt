package io.javalin.core.compression

import java.io.OutputStream
import java.util.zip.GZIPOutputStream


/**
 * Kotlin wrapper for Java's GZIPOutputStream.
 * DynamicCompressionStrategy uses this to perform Gzip compression
 * @see DynamicCompressionStrategy
 *
 * @param out the output stream
 * @param syncFlush
 *        if {@code true} invocation of the inherited
 *        {@link DeflaterOutputStream#flush() flush()} method of
 *        this instance flushes the compressor with flush mode
 *        {@link Deflater#SYNC_FLUSH} before flushing the output
 *        stream, otherwise only flushes the output stream
 * @param level Compression level. Range 0..9, default = 6
 */
class GzipWrapper(val out: OutputStream,
                  val syncFlush: Boolean = false,
                  val level: Int = 6): GZIPOutputStream(out, syncFlush) {

    init {
        require(level in 0..9) {
            "Valid range for parameter level is 0 to 9"
        }
        this.def.setLevel(level)
    }
}
