package io.javalin.compression

import com.nixxcode.jvmbrotli.enc.BrotliOutputStream
import com.nixxcode.jvmbrotli.enc.Encoder
import io.javalin.compression.CompressionType.BR
import io.javalin.compression.CompressionType.GZIP
import io.javalin.http.Context
import io.javalin.http.Header
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

data class CompressedStream(val type: CompressionType, val outputStream: OutputStream) {
    companion object {
        fun tryBrotli(compression: CompressionStrategy, ctx: Context): CompressedStream? = when {
            compression.brotli == null -> null
            ctx.acceptsHeader?.contains(BR.typeName, ignoreCase = true) != true -> null
            else -> CompressedStream(BR, LeveledBrotliStream(ctx.res().outputStream, compression.brotli.level))
        }
        fun tryGzip(compression: CompressionStrategy, ctx: Context): CompressedStream? = when {
            compression.gzip == null -> null
            ctx.acceptsHeader?.contains(GZIP.typeName, ignoreCase = true) != true -> null
            else -> CompressedStream(GZIP, LeveledGzipStream(ctx.res().outputStream, compression.gzip.level))
        }
    }
}

private val Context.acceptsHeader: String? get() = this.header(Header.ACCEPT_ENCODING)

class LeveledGzipStream(out: OutputStream, level: Int) : GZIPOutputStream(out) {
    init {
        this.def.setLevel(level)
    }
}

class LeveledBrotliStream(out: OutputStream, level: Int) :
    BrotliOutputStream(out, Encoder.Parameters().setQuality(level))
