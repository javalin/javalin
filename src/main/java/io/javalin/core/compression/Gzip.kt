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

    init {
        require(level in 0..9) { "Valid range for parameter level is 0 to 9" }
    }

    /**
     * @param out The target output stream
     * @param data data to compress
     */
    fun compress(data: ByteArrayOutputStream) : ByteArray {
        val dummyOut = object : OutputStream() {
            val interceptedDataStream = ByteArrayOutputStream()
            override fun write(b: ByteArray) {
                interceptedDataStream.write(b)
            }
            override fun write(b: Int) {
                interceptedDataStream.write(b)
            }
        }

        //object is required so we can set level, because def is a protected field
        val wrapper = object : GZIPOutputStream(dummyOut, true) {
            init {
                this.def.setLevel(level)
            }
        }
        wrapper.use { it.write(data.toByteArray()) }
        return dummyOut.interceptedDataStream.toByteArray()
    }
}
