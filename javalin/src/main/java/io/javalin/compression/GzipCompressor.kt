package io.javalin.compression

import java.io.OutputStream
import java.util.zip.GZIPOutputStream

/** @param level Compression level. Higher yields better (but slower) compression. Range 0..9, default = 6
 *  @param bufferSize Buffer size for compression stream. Default = null (uses default buffer size) */
class GzipCompressor(val level: Int, val bufferSize: Int? = null) : Compressor {
    init {
        require(level in 0..9) { "Valid range for parameter level is 0 to 9" }
        require(bufferSize == null || bufferSize > 0) { "Buffer size must be positive" }
    }

    override fun encoding() = CompressionType.GZIP.typeName
    override fun extension() = CompressionType.GZIP.extension
    override fun compress(out: OutputStream) = LeveledGzipStream(out, level, bufferSize)
}

class LeveledGzipStream(out: OutputStream, level: Int, bufferSize: Int?) : GZIPOutputStream(
    out, bufferSize ?: 512 // GZIPOutputStream default is 512 bytes
) {
    init {
        this.def.setLevel(level)
    }
}
