package io.javalin.compression

import com.nixxcode.jvmbrotli.enc.BrotliOutputStream
import com.nixxcode.jvmbrotli.enc.Encoder
import io.javalin.compression.CompressionType.BR
import io.javalin.compression.CompressionType.GZIP
import io.javalin.http.Context
import io.javalin.http.Header
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

data class CompressedStream(val type: io.javalin.compression.CompressionType, val outputStream: OutputStream) {
    companion object {
        fun tryBrotli(compression: io.javalin.compression.CompressionStrategy, ctx: Context): io.javalin.compression.CompressedStream? =
            if (compression.brotli != null && ctx.header(Header.ACCEPT_ENCODING)?.contains(BR.typeName, ignoreCase = true) == true)
                io.javalin.compression.CompressedStream(
                    BR,
                    io.javalin.compression.LeveledBrotliStream(ctx.res.outputStream, compression.brotli.level)
                )
            else null

        fun tryGzip(compression: io.javalin.compression.CompressionStrategy, ctx: Context): io.javalin.compression.CompressedStream? =
            if (compression.gzip != null && ctx.header(Header.ACCEPT_ENCODING)?.contains(GZIP.typeName, ignoreCase = true) == true)
                io.javalin.compression.CompressedStream(
                    GZIP,
                    io.javalin.compression.LeveledGzipStream(ctx.res.outputStream, compression.gzip.level)
                )
            else null
    }
}

class LeveledGzipStream(out: OutputStream, level: Int) : GZIPOutputStream(out) {
    init {
        this.def.setLevel(level)
    }
}

class LeveledBrotliStream(out: OutputStream, level: Int) : BrotliOutputStream(out, Encoder.Parameters().setQuality(level))
