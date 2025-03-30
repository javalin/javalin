package io.javalin.http.sse

import jakarta.servlet.http.HttpServletResponse
import java.io.IOException
import java.io.InputStream

const val COMMENT_PREFIX = ":"
const val NEW_LINE = "\n"
// before refactoring
class Emitter(private var response: HttpServletResponse) {

    var closed = false
        private set
// method to be refactored
fun emit(event: String, data: InputStream, id: String?) = synchronized(this) {
    try {
        writeSseHeaders(event, id) // Extracted
        writeSseData(data)        // Extracted
        write(NEW_LINE)
        response.flushBuffer()
    } catch (ignored: IOException) {
        closed = true
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
// marked for refactoring
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
