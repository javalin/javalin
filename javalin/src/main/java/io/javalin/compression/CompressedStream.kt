package io.javalin.compression

import com.nixxcode.jvmbrotli.enc.BrotliOutputStream
import com.nixxcode.jvmbrotli.enc.Encoder
import io.javalin.compression.CompressionType.BR
import io.javalin.compression.CompressionType.GZIP
import io.javalin.http.Context
import io.javalin.http.Header
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.WriteListener
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

internal class CompressedOutputStream(val compression: CompressionStrategy, val ctx: Context) : ServletOutputStream() {

    private val originStream = ctx.res().outputStream
    private var compressedStream: OutputStream? = null

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (compressedStream == null && length >= compression.minSizeForCompression && compression.allowsForCompression(ctx.res().contentType) && !ctx.res().containsHeader(Header.CONTENT_ENCODING)) {
            tryMatchCompression(compression, ctx, originStream)?.also { (type, stream) ->
                this.compressedStream = stream
                ctx.header(Header.CONTENT_ENCODING, type.typeName)
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

private fun tryMatchCompression(compression: CompressionStrategy, ctx: Context, originStream: OutputStream): Pair<CompressionType, OutputStream>? =
    ctx.header(Header.ACCEPT_ENCODING)?.let { acceptedEncoding ->
        tryBrotli(compression, originStream, acceptedEncoding) ?: tryGzip(compression, originStream, acceptedEncoding)
    }

private fun tryBrotli(compression: CompressionStrategy, originStream: OutputStream, acceptedEncoding: String): Pair<CompressionType, OutputStream>? = when {
    compression.brotli == null -> null
    !acceptedEncoding.contains(BR.typeName, ignoreCase = true) -> null
    else -> BR to LeveledBrotliStream(originStream, compression.brotli.level)
}

private fun tryGzip(compression: CompressionStrategy, originStream: OutputStream, acceptedEncoding: String): Pair<CompressionType, OutputStream>? = when {
    compression.gzip == null -> null
    !acceptedEncoding.contains(GZIP.typeName, ignoreCase = true) -> null
    else -> GZIP to LeveledGzipStream(originStream, compression.gzip.level)
}

class LeveledGzipStream(out: OutputStream, level: Int) : GZIPOutputStream(out) {
    init {
        this.def.setLevel(level)
    }
}

class LeveledBrotliStream(out: OutputStream, level: Int) :
    BrotliOutputStream(out, Encoder.Parameters().setQuality(level))

private fun CompressionStrategy.allowsForCompression(contentType: String?): Boolean =
    contentType == null || excludedMimeTypesFromCompression.none { excluded -> contentType.contains(excluded, ignoreCase = true) }
