package io.javalin.compression

/** Configuration for Gzip compression
 * @param level Compression level. Higher yields better (but slower) compression. Range 0..9, default = 6
 * @param bufferSize Buffer size for compression stream. Default = null (uses system default or configured value) */
class Gzip @JvmOverloads constructor(val level: Int = 6, val bufferSize: Int? = null) {
    init {
        require(level in 0..9) { "Valid range for parameter level is 0 to 9" }
        require(bufferSize == null || bufferSize > 0) { "Buffer size must be positive" }
    }
}
