package io.javalin.core.compression

/**
 * @param level Compression level. Higher yields better (but slower) compression. Range 0..11, default = 4
 */
@Deprecated("WARNING: Brotli compression is an experimental feature!")
class Brotli(val level: Int = 4) {
    init {
        require(level in 0..11) { "Valid range for parameter level is 0 to 11" }
    }
}
