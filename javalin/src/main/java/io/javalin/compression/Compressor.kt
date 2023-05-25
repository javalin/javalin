package io.javalin.compression

import java.io.OutputStream

/** A compressor is used to compress an output stream */
interface Compressor {

    /** The content encoding for this compressor (e.g. "gzip")
     * [MDN Content-Encoding](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Encoding)
     * @return the content encoding (case-insensitive) */
    fun encoding(): String

    /** @return the file extension for this compressor (empty string if none) */
    fun extension(): String = ""

    /** @param out the output stream to compress
     * @return the compressed output stream */
    fun compress(out: OutputStream): OutputStream
}
