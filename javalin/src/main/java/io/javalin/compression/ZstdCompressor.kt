package io.javalin.compression

import com.github.luben.zstd.ZstdOutputStream
import java.io.OutputStream

/** @param level Compression level. Higher yields better (but slower) compression. Range 0..22, default = 3 */
class ZstdCompressor(val level: Int) : Compressor {
    init {
        require(level in 0..22) { "Valid range for parameter level is 0 to 22" }
    }

    override fun encoding(): String = CompressionType.ZSTD.typeName
    override fun extension(): String = CompressionType.ZSTD.extension
    override fun compress(out: OutputStream): OutputStream = ZstdOutputStream(out, level)
}