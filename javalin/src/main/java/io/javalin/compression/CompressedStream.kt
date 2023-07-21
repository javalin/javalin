package io.javalin.compression

import io.javalin.http.Context
import io.javalin.http.Header
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import java.io.OutputStream

internal class CompressedOutputStream(val compression: CompressionStrategy, val ctx: Context) : ServletOutputStream() {

    companion object {
        const val ALWAYS_COMPRESS = "ALWAYS_COMPRESS"
    }

    private val originStream = ctx.res().outputStream
    private var compressedStream: OutputStream? = null
    private var isCompressionDecisionMade = false

    private fun maybeCreateCompressionStreamOnFirstWrite(length: Int) {
        if (!isCompressionDecisionMade) {
            val isCompressionAllowed = !ctx.res().containsHeader(Header.CONTENT_ENCODING) &&
                compression.allowsForCompression(ctx.res().contentType)
            val isCompressionDesired = length >= compression.minSizeForCompression ||
                ctx.attribute<Boolean>(ALWAYS_COMPRESS) == true
            if (isCompressionAllowed && isCompressionDesired) {
                findMatchingCompressor(compression, ctx)?.also {
                    this.compressedStream = it.compress(originStream)
                    ctx.header(Header.CONTENT_ENCODING, it.encoding())
                }
            }
            isCompressionDecisionMade = true
        }
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        maybeCreateCompressionStreamOnFirstWrite(length)
        (compressedStream ?: originStream).write(bytes, offset, length)
    }

    override fun write(byte: Int) {
        maybeCreateCompressionStreamOnFirstWrite(1)
        (compressedStream ?: originStream).write(byte)
    }

    override fun setWriteListener(writeListener: WriteListener?) = originStream.setWriteListener(writeListener)
    override fun isReady(): Boolean = originStream.isReady
    override fun close() {
        compressedStream?.close()
    }

}

private fun findMatchingCompressor(
    compression: CompressionStrategy,
    ctx: Context,
): Compressor? =
    ctx.header(Header.ACCEPT_ENCODING)?.let { acceptedEncoding ->
        compression.compressors.firstOrNull { acceptedEncoding.contains(it.encoding(), ignoreCase = true) }
    }


private fun CompressionStrategy.allowsForCompression(contentType: String?): Boolean =
    contentType == null || excludedMimeTypesFromCompression.none { excluded ->
        contentType.contains(excluded, ignoreCase = true)
    }
