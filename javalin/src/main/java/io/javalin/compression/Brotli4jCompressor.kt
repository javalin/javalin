package io.javalin.compression


import com.aayushatharva.brotli4j.encoder.BrotliOutputStream
import com.aayushatharva.brotli4j.encoder.Encoder
import java.io.OutputStream

/** @param level Compression level. Higher yields better (but slower) compression. Range 0..11, default = 4
 *  @param bufferSize Buffer size for compression stream. Default = null (uses default buffer size) */
class Brotli4jCompressor(val level: Int, val bufferSize: Int? = null) : Compressor {
    init {
        require(level in 0..11) { "Valid range for parameter level is 0 to 11" }
        require(bufferSize == null || bufferSize > 0) { "Buffer size must be positive" }
    }

    override fun encoding(): String = CompressionType.BR.typeName
    override fun extension(): String = CompressionType.BR.extension
    override fun compress(out: OutputStream): OutputStream = LeveledBrotli4jStream(out, level, bufferSize)
}

class LeveledBrotli4jStream(out: OutputStream, level: Int, bufferSize: Int?) :
    BrotliOutputStream(out, Encoder.Parameters().setQuality(level), bufferSize ?: 16384) // BrotliOutputStream default is 16384 bytes
