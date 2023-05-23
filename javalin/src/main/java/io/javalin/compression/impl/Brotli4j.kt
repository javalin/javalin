package io.javalin.compression.impl


import com.aayushatharva.brotli4j.encoder.BrotliOutputStream
import com.aayushatharva.brotli4j.encoder.Encoder
import io.javalin.compression.CompressionType
import io.javalin.compression.Compressor
import java.io.OutputStream

/** @param level Compression level. Higher yields better (but slower) compression. Range 0..11, default = 4 */
class Brotli4j(val level: Int) : Compressor {
    init {
        require(level in 0..11) { "Valid range for parameter level is 0 to 11" }
    }

    override fun type(): CompressionType {
        return CompressionType.BR
    }

    override fun compress(out: OutputStream): OutputStream {
        return LeveledBrotli4jStream(out, level)
    }
}

private class LeveledBrotli4jStream(out: OutputStream, level: Int) :
    BrotliOutputStream(out, Encoder.Parameters().setQuality(level))
