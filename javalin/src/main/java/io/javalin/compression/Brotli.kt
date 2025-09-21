package io.javalin.compression

/** Configuration for Brotli compression
 * @param level Compression level. Higher yields better (but slower) compression. Range 0..11, default = 4
 * @param bufferSize Buffer size for compression stream. Default = null (uses system default or configured value) */
class Brotli(val level: Int = 4, val bufferSize: Int? = null) {
    init {
        require(level in 0..11) { "Valid range for parameter level is 0 to 11" }
        require(bufferSize == null || bufferSize > 0) { "Buffer size must be positive" }
    }
}
