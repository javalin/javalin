package io.javalin.core.compression

import java.io.OutputStream
import java.util.zip.GZIPOutputStream

/**
 * DynamicCompressionHandler uses this to perform Gzip compression
 * @see DynamicCompressionHandler
 *
 * @param level Compression level. Higher yields better (but slower) compression. Range 0..9, default = 6
 */
class Gzip(val level: Int = 6) {

    init {
        require(level in 0..9) {
            "Valid range for parameter level is 0 to 9"
        }
    }

    /**
     * @param out The target output stream
     * @param data data to compress
     */
    fun write(out: OutputStream, data: ByteArray) {
        GzipWrapper(out, level).use {
            it.write(data)
        }
    }
}

/**
 * Kotlin wrapper for Java's GZIPOutputStream. Required so we can set gzip compression level
 */
class GzipWrapper(out: OutputStream, level: Int = 6): GZIPOutputStream(out, true) {

    init {
        require(level in 0..9) {
            "Valid range for parameter level is 0 to 9"
        }
        this.def.setLevel(level)
    }
}
