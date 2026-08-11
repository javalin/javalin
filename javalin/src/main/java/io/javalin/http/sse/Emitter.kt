package io.javalin.http.sse

import jakarta.servlet.http.HttpServletResponse
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

const val COMMENT_PREFIX = ":"
const val NEW_LINE = "\n"

class Emitter(private var response: HttpServletResponse) {

    private val lock = ReentrantLock()

    var closed = false
        private set

    fun emit(event: String?, data: InputStream, id: String?) = lock.withLock {
        try {
            // Strip CR/LF from event and id so attacker-controlled values
            // can't inject extra SSE frames — see #2579.
            val sanitizedId = id?.let(::stripCrLf)
            val sanitizedEvent = event?.let(::stripCrLf)
            if (sanitizedId != null) {
                write("id: $sanitizedId$NEW_LINE")
            }
            if (sanitizedEvent != null) {
                write("event: $sanitizedEvent$NEW_LINE")
            }
            data.buffered().reader().useLines {
                it.forEach { line -> write("data: $line$NEW_LINE") }
            }

            write(NEW_LINE)
            response.flushBuffer()
        } catch (ignored: IOException) {
            closed = true
        }
    }

    private fun stripCrLf(value: String): String =
        value.replace("\r", "").replace("\n", "")

    fun emit(comment: String) =
        try {
            comment.split(NEW_LINE).forEach {
                write("$COMMENT_PREFIX $it$NEW_LINE")
            }
            response.flushBuffer()
        } catch (ignored: IOException) {
            closed = true
        }

    private fun write(value: String) =
        response.outputStream.print(value)

}
