package io.javalin.http.util

import io.javalin.core.util.Header
import io.javalin.http.Context
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

object SeekableWriter {
    var chunkSize = 128000
    fun write(ctx: Context, inputStream: InputStream, contentType: String, totalBytes: Long) {
        if (ctx.header(Header.RANGE) == null) {
            ctx.header(Header.CONTENT_TYPE, contentType)
            ctx.result(inputStream)
            return
        }
        val requestedRange = ctx.header(Header.RANGE)!!.split("=")[1].split("-").filter { it.isNotEmpty() }
        val from = requestedRange[0].toLong()
        val to = when {
            from + chunkSize > totalBytes -> totalBytes - 1 // chunk bigger than file, write all
            requestedRange.size == 2 -> requestedRange[1].toLong() // chunk smaller than file, to/from specified
            else -> from + chunkSize - 1 // chunk smaller than file, to/from not specified
        }
        ctx.status(206)
        ctx.header(Header.CONTENT_TYPE, contentType)
        ctx.header(Header.ACCEPT_RANGES, "bytes")
        ctx.header(Header.CONTENT_RANGE, "bytes $from-$to/$totalBytes")
        ctx.header(Header.CONTENT_LENGTH, "${min(to - from + 1, totalBytes)}")
        ctx.res.outputStream.write(inputStream, from, to)
    }

    private fun OutputStream.write(inputStream: InputStream, from: Long, to: Long, buffer: ByteArray = ByteArray(1024)) = inputStream.use {
        var toSkip = from
        while (toSkip > 0) {
            val skipped = it.skip(toSkip)
            toSkip -= skipped
        }
        var bytesLeft = to - from + 1
        while (bytesLeft != 0L) {
            val read = it.read(buffer, 0, buffer.size.toLong().coerceAtMost(bytesLeft).toInt())
            this.write(buffer, 0, read)
            bytesLeft -= read
        }
    }
}
