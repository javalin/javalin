package io.javalin.http.util

import io.javalin.core.util.Header
import io.javalin.http.Context
import java.io.InputStream
import java.io.OutputStream

object SeekableWriter {
    var chunkSize = 128000
    fun write(ctx: Context, inputStream: InputStream, contentType: String) {
        if (ctx.header(Header.RANGE) == null) {
            ctx.header(Header.CONTENT_TYPE, contentType)
            ctx.result(inputStream)
            return
        }
        val totalBytes = inputStream.available()
        val requestedRange = ctx.header(Header.RANGE)!!.split("=")[1].split("-").filter { it.isNotEmpty() }
        val from = requestedRange[0].toInt()
        val to = when {
            from + chunkSize > totalBytes -> totalBytes - 1 // chunk bigger than file, write all
            requestedRange.size == 2 -> requestedRange[1].toInt() // chunk smaller than file, to/from specified
            else -> from + chunkSize - 1 // chunk smaller then file, to/from not specified
        }
        ctx.status(206)
        ctx.header(Header.CONTENT_TYPE, contentType)
        ctx.header(Header.ACCEPT_RANGES, "bytes")
        ctx.header(Header.CONTENT_RANGE, "bytes $from-$to/$totalBytes")
        ctx.header(Header.CONTENT_LENGTH, "${to - from + 1}")
        ctx.res.outputStream.write(inputStream, from, to)
    }

    private fun OutputStream.write(inputStream: InputStream, from: Int, to: Int, buffer: ByteArray = ByteArray(1024)) = inputStream.use {
        it.skip(from.toLong())
        var bytesLeft = to - from + 1
        while (bytesLeft != 0) {
            val read = it.read(buffer, 0, Math.min(buffer.size, bytesLeft))
            this.write(buffer, 0, read)
            bytesLeft -= read
        }
    }
}
