package io.javalin.compression.impl


import com.nixxcode.jvmbrotli.enc.BrotliOutputStream
import com.nixxcode.jvmbrotli.enc.Encoder
import io.javalin.compression.CompressionType
import io.javalin.compression.Compressor
import java.io.OutputStream

/** @param level Compression level. Higher yields better (but slower) compression. Range 0..11, default = 4 */
class BrotliJvm(val level: Int) : Compressor {
    init {
        require(level in 0..11) { "Valid range for parameter level is 0 to 11" }
    }

    override fun type(): CompressionType {
        return CompressionType.BR
    }

    override fun compress(out: OutputStream): OutputStream {
        return LeveledBrotliJvmStream(out, level)
    }
}

class LeveledBrotliJvmStream(out: OutputStream, level: Int) :
    BrotliOutputStream(out, Encoder.Parameters().setQuality(level))
