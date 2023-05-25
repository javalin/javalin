package io.javalin.compression

import io.javalin.http.Context
import io.javalin.http.Header
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import java.io.OutputStream

internal class CompressedOutputStream(val compression: CompressionStrategy, val ctx: Context) : ServletOutputStream() {

    private val originStream = ctx.res().outputStream
    private var compressedStream: OutputStream? = null

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (compressedStream == null && length >= compression.minSizeForCompression && compression.allowsForCompression(ctx.res().contentType) && !ctx.res().containsHeader(Header.CONTENT_ENCODING)) {
            tryMatchCompression(compression, ctx)?.also {
                this.compressedStream = it.compress(originStream)
                ctx.header(Header.CONTENT_ENCODING, it.encoding())
            }
        }
        (compressedStream ?: originStream).write(bytes, offset, length) // fall back to default stream if no compression
    }

    override fun write(byte: Int) = originStream.write(byte)
    override fun setWriteListener(writeListener: WriteListener?) = originStream.setWriteListener(writeListener)
    override fun isReady(): Boolean = originStream.isReady
    override fun close() {
        compressedStream?.close()
    }

}

private fun tryMatchCompression(
    compression: CompressionStrategy,
    ctx: Context,
): Compressor? =
    ctx.header(Header.ACCEPT_ENCODING)?.let { acceptedEncoding ->
        compression.compressors.firstOrNull { acceptedEncoding.contains(it.encoding(), ignoreCase = true) }
    }


private fun CompressionStrategy.allowsForCompression(contentType: String?): Boolean =
    contentType == null || excludedMimeTypesFromCompression.none { excluded -> contentType.contains(excluded, ignoreCase = true) }

