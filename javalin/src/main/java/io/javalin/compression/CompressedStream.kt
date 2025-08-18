package io.javalin.compression

import io.javalin.http.Context
import io.javalin.http.Header
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import java.io.OutputStream
import java.util.Locale.getDefault

internal class CompressedOutputStream(
    val minSizeForCompression: Int,
    val compression: CompressionStrategy,
    val ctx: Context,
) : ServletOutputStream() {

    private val originStream = ctx.res().outputStream
    private var compressedStream: OutputStream? = null
    private var isCompressionDecisionMade = false

    private fun maybeCreateCompressionStreamOnFirstWrite(length: Int) {
        if (!isCompressionDecisionMade) {
            val isCompressionAllowed = !ctx.res().containsHeader(Header.CONTENT_ENCODING) &&
                compression.allowsForCompression(ctx.res().contentType)
            val isCompressionDesired = length >= minSizeForCompression
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
    compressionStrategy: CompressionStrategy,
    ctx: Context,
): Compressor? =
    ctx.header(Header.ACCEPT_ENCODING)?.let { acceptedEncoding ->
        val acceptedEncodingLowerCase = acceptedEncoding.lowercase(getDefault())

        for (preferredCompressor in compressionStrategy.preferredCompressors) {
            if(acceptedEncodingLowerCase.contains(preferredCompressor.typeName)) {
                val compressor = compressionStrategy.compressors.forType(preferredCompressor.typeName)

                if(compressor != null) {
                    return compressor
                }
            }
        }

        return compressionStrategy.compressors.firstOrNull { acceptedEncodingLowerCase.contains(it.encoding(), ignoreCase = true) }
    }


private fun CompressionStrategy.allowsForCompression(contentType: String?): Boolean =
    contentType == null || allowedMimeTypes.contains(contentType) || excludedMimeTypes.none { excluded ->
        contentType.contains(excluded, ignoreCase = true)
    }
