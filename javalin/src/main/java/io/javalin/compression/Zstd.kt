package io.javalin.compression

/** Configuration for Zstd compression
 * @param level Compression level. Higher yields better (but slower) compression. Range 0..22, default = 3 */
class Zstd(val level: Int = 3) {
    init {
        require(level in 0..22) { "Valid range for parameter level is 0 to 22" }
    }
}