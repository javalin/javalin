package io.javalin.compression

import java.io.OutputStream

interface Compressor {

    fun type(): CompressionType

    fun outputStream(out: OutputStream): OutputStream
}
