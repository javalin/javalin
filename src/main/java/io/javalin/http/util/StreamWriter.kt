package io.javalin.http.util

import io.javalin.core.util.Header
import io.javalin.http.Context
import java.io.InputStream

object StreamWriter {
    private const val chunkSize = 256000
    fun write(ctx: Context, inputStream: InputStream, contentType: String) {
        if (ctx.header(Header.RANGE) == null) {
            ctx.result(inputStream)
            return
        }
        val ranges = ctx.header(Header.RANGE)!!.split("=")[1].split("-").filter { it.isNotEmpty() }
        val from = ranges[0].toInt()
        var to = chunkSize + from
        val fileLength = inputStream.available()
        if (to > fileLength) {
            to = fileLength - 1
        }
        if (ranges.size == 2) {
            to = ranges[1].toInt()
        }
        var length = to - from + 1
        ctx.status(206)
        ctx.header(Header.CONTENT_TYPE, contentType)
        ctx.header(Header.ACCEPT_RANGES, "bytes")
        ctx.header(Header.CONTENT_RANGE, "bytes $from-$to/$fileLength")
        ctx.header(Header.CONTENT_LENGTH, "$length")
        val buffer = ByteArray(1024)
        inputStream.apply { skip(from.toLong()) }.use {
            while (length != 0) {
                val read = it.read(buffer, 0, Math.min(buffer.size, length))
                ctx.res.outputStream.write(buffer, 0, read)
                length -= read
            }
        }
    }
}
