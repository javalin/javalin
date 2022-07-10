package io.javalin.core.compression

import com.nixxcode.jvmbrotli.enc.BrotliOutputStream
import com.nixxcode.jvmbrotli.enc.Encoder
import io.javalin.core.compression.CompressionType.BR
import io.javalin.core.compression.CompressionType.GZIP
import io.javalin.http.Header
import io.javalin.http.Context
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

data class CompressedStream(val type: CompressionType, val outputStream: OutputStream) {
    companion object {
        fun tryBrotli(compression: CompressionStrategy, ctx: Context): CompressedStream? =
            if (compression.brotli != null && ctx.header(Header.ACCEPT_ENCODING)?.contains(BR.typeName, ignoreCase = true) == true)
                CompressedStream(BR, LeveledBrotliStream(ctx.res.outputStream, compression.brotli.level))
            else null

        fun tryGzip(compression: CompressionStrategy, ctx: Context): CompressedStream? =
            if (compression.gzip != null && ctx.header(Header.ACCEPT_ENCODING)?.contains(GZIP.typeName, ignoreCase = true) == true)
                CompressedStream(GZIP, LeveledGzipStream(ctx.res.outputStream, compression.gzip.level))
            else null
    }
}

class LeveledGzipStream(out: OutputStream, level: Int) : GZIPOutputStream(out) {
    init {
        this.def.setLevel(level)
    }
}

class LeveledBrotliStream(out: OutputStream, level: Int) : BrotliOutputStream(out, Encoder.Parameters().setQuality(level))
