package io.javalin.http.sse

import jakarta.servlet.http.HttpServletResponse
import java.io.IOException
import java.io.InputStream

const val COMMENT_PREFIX = ":"
const val NEW_LINE = "\n"

class Emitter(private val response: HttpServletResponse) {

    private var closed = false
    private var closeListener: (() -> Unit)? = null

    fun onClose(listener: () -> Unit) {
        closeListener = listener
    }

    @Throws(IOException::class)
    fun emit(event: String, data: InputStream, id: String?) = synchronized(this) {
        try {
            writeSseHeaders(event, id)
            writeSseData(data)
            write(NEW_LINE)
            flushResponse()
        } catch (e: IOException) {
            handleConnectionClosed()
            throw e
        }
    }

    @Throws(IOException::class)
    fun emit(comment: String) {
        try {
            val commentLinePrefix = "$COMMENT_PREFIX"
            comment.split(NEW_LINE).forEach {
                write("$commentLinePrefix $it$NEW_LINE")
            }
            flushResponse()
        } catch (e: IOException) {
            handleConnectionClosed()
            throw e
        }
    }

    private fun writeSseHeaders(event: String, id: String?) {
        id?.let { write("id: $it$NEW_LINE") }
        write("event: $event$NEW_LINE")
    }

    private fun writeSseData(data: InputStream) {
        data.buffered().reader().useLines {
            it.forEach { line -> write("data: $line$NEW_LINE") }
        }
    }

    private fun write(value: String) {
        response.outputStream.print(value)
    }

    private fun flushResponse() {
        response.flushBuffer()
    }

    private fun handleConnectionClosed() {
        closed = true
        closeListener?.invoke()
    }
}
