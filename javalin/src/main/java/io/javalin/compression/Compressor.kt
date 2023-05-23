package io.javalin.compression

import java.io.OutputStream

/** A compressor is used to compress an output stream */
interface Compressor {

    /**  @return the compression type for this compressor */
    fun type(): CompressionType

    /** @param out the output stream to compress
     * @return the compressed output stream */
    fun compress(out: OutputStream): OutputStream
}
