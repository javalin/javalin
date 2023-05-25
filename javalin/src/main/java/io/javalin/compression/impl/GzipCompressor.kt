package io.javalin.compression.impl

import io.javalin.compression.CompressionType
import io.javalin.compression.Compressor
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

/** @param level Compression level. Higher yields better (but slower) compression. Range 0..9, default = 6 */
class GzipCompressor(val level: Int) : Compressor {
    init {
        require(level in 0..9) { "Valid range for parameter level is 0 to 9" }
    }

    override fun encoding(): String {
        return CompressionType.GZIP.typeName
    }

    override fun extension(): String {
        return CompressionType.GZIP.extension
    }

    override fun compress(out: OutputStream): OutputStream {
        return LeveledGzipStream(out, level)
    }
}

class LeveledGzipStream(out: OutputStream, level: Int) : GZIPOutputStream(out) {
    init {
        this.def.setLevel(level)
    }
}
