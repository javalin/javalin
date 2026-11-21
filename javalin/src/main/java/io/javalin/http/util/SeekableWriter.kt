package io.javalin.http.util

import io.javalin.http.Context
import io.javalin.http.Header
import io.javalin.http.HttpStatus
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.min

object SeekableWriter {
    var chunkSize = 128000
    fun write(ctx: Context, inputStream: InputStream, contentType: String, totalBytes: Long) = ctx.async {
        val uncompressedStream = ctx.res().outputStream
        val isAudioOrVideoFile = contentType.isAudioOrVideo()
        ctx.header(Header.ACCEPT_RANGES, "bytes")
        val requestedRange = ctx.header(Header.RANGE)?.let { parseRange(it, totalBytes) }
        if (requestedRange == null) {
            ctx.header(Header.CONTENT_TYPE, contentType)
            ctx.header(Header.CONTENT_LENGTH, "$totalBytes")
            inputStream.transferTo(uncompressedStream)
            inputStream.close()
            return@async
        }
        if (requestedRange == UNSATISFIABLE) {
            inputStream.close()
            ctx.status(HttpStatus.RANGE_NOT_SATISFIABLE)
            ctx.header(Header.CONTENT_RANGE, "bytes */$totalBytes")
            return@async
        }
        val (from, requestedTo) = requestedRange
        val to = when {
            isAudioOrVideoFile -> when {
                from + chunkSize > totalBytes -> totalBytes - 1 // chunk bigger than file, write all
                requestedTo != null -> requestedTo // chunk smaller than file, to/from specified
                else -> from + chunkSize - 1
            }
            else -> (totalBytes - 1)
        }
        val contentLength = when {
            isAudioOrVideoFile -> min(to - from + 1, totalBytes)
            else -> (totalBytes - from)
        }

        val status = when {
            isAudioOrVideoFile -> HttpStatus.PARTIAL_CONTENT
            else -> HttpStatus.OK
        }

        ctx.status(status)
        ctx.header(Header.CONTENT_TYPE, contentType)
        ctx.header(Header.ACCEPT_RANGES, "bytes")
        ctx.header(Header.CONTENT_RANGE, "bytes $from-$to/$totalBytes")
        ctx.header(Header.CONTENT_LENGTH, contentLength.toString())
        uncompressedStream.write(inputStream, from, to)
    }

    private fun OutputStream.write(inputStream: InputStream, from: Long, to: Long, buffer: ByteArray = ByteArray(1024)) = inputStream.use {
        it.skipNBytes(from)
        var bytesLeft = to - from + 1
        while (bytesLeft > 0) {
            val read = it.read(buffer, 0, buffer.size.toLong().coerceAtMost(bytesLeft).toInt())
            if (read == -1) break
            this.write(buffer, 0, read)
            bytesLeft -= read
        }
    }

    private val UNSATISFIABLE = -1L to null

    /**
     * Parses a single byte range (RFC 9110, section 14.1.2) into its first byte and, unless it is open-ended,
     * its last byte (capped to the end of the stream). Returns null if the header should be ignored (another
     * unit, several ranges or invalid syntax) and [UNSATISFIABLE] if no byte of the stream is in the range.
     */
    private fun parseRange(header: String, totalBytes: Long): Pair<Long, Long?>? {
        val (unit, spec) = header.split("=", limit = 2).takeIf { it.size == 2 } ?: return null
        if (!unit.trim().equals("bytes", ignoreCase = true) || spec.contains(",")) return null
        val (first, last) = spec.trim().split("-", limit = 2).takeIf { it.size == 2 } ?: return null
        if ((first + last).any { it !in '0'..'9' } || (first.isEmpty() && last.isEmpty())) return null
        // a position too large for a Long is past the end of any stream
        val firstByte = if (first.isEmpty()) null else first.toLongOrNull() ?: Long.MAX_VALUE
        val lastByte = if (last.isEmpty()) null else last.toLongOrNull() ?: Long.MAX_VALUE
        return when {
            firstByte == null -> when { // suffix range: the last n bytes
                lastByte == 0L || totalBytes == 0L -> UNSATISFIABLE
                else -> (totalBytes - minOf(lastByte!!, totalBytes)) to (totalBytes - 1)
            }
            firstByte >= totalBytes -> UNSATISFIABLE
            lastByte == null -> firstByte to null
            lastByte < firstByte -> null
            else -> firstByte to minOf(lastByte, totalBytes - 1)
        }
    }

    private fun String.isAudioOrVideo(): Boolean {
        return this.startsWith("audio/") || this.startsWith("video/")
    }
}
