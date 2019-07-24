package io.javalin.core.compression

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

/**
 * Kotlin wrapper for Java's GZIPOutputStream
 *
 * @param level Compression level. Higher yields better (but slower) compression. Range 0..9, default = 6
 */
class Gzip(val level: Int = 6) {

    lateinit var compressor: GZIPOutputStream

    init {
        require(level in 0..9) { "Valid range for parameter level is 0 to 9" }
    }

    fun create(out: OutputStream) { // create new instance based on current output stream
        // object is required so we can set level, because def is a protected field
        compressor = object : GZIPOutputStream(out, true) {
            init {
                this.def.setLevel(level)
            }
        }
    }

    fun write(data: ByteArray, off: Int, len: Int) {
        compressor.write(data, off, len)
    }

    fun write(data: ByteArrayOutputStream) {
        write(data.toByteArray(), 0, data.size())
    }

    fun finish() { // this is important so the compressor can write the gzip "tail", indicating end of stream
        compressor.finish()
    }
}
