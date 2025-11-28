package io.javalin.http.servlet

import io.javalin.config.Key
import io.javalin.http.Context
import io.javalin.http.HttpResponseException
import io.javalin.http.HttpStatus
import io.javalin.util.JavalinLogger
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal object MaxRequestSize {
    val MaxRequestSizeKey = Key<Long>("javalin-max-request-size")

    fun readBytesWithLimit(inputStream: InputStream, maxRequestSize: Long): ByteArray {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var totalRead = 0L
        var bytesRead: Int

        while (inputStream.read(chunk).also { bytesRead = it } != -1) {
            totalRead += bytesRead
            if (totalRead > maxRequestSize) {
                JavalinLogger.warn("Body size greater than max size ($maxRequestSize bytes)")
                throw HttpResponseException(HttpStatus.CONTENT_TOO_LARGE, HttpStatus.CONTENT_TOO_LARGE.message)
            }
            buffer.write(chunk, 0, bytesRead)
        }

        return buffer.toByteArray()
    }
}
