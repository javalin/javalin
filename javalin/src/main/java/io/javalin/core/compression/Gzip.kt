package io.javalin.core.compression

/**
 * @param level Compression level. Higher yields better (but slower) compression. Range 0..9, default = 6
 */
class Gzip(val level: Int = 6) {
    init {
        require(level in 0..9) { "Valid range for parameter level is 0 to 9" }
    }
}
