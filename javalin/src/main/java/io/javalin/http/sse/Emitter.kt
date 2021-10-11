package io.javalin.http.sse

import java.io.IOException
import java.io.InputStream
import javax.servlet.AsyncContext
import javax.servlet.ServletOutputStream

const val COMMENT_PREFIX = ":"

class Emitter(private var asyncContext: AsyncContext) {

    private lateinit var output: ServletOutputStream
    private var closed = false
    private val newline = "\n"

    init {
        try {
            this.output = asyncContext.response.outputStream
        } catch (e: IOException) {
            closed = true
        }
    }

    fun emit(event: String, data: InputStream, id: String?) = synchronized(this) {
        try {
            if (id != null) {
                output.print("id: $id$newline")
            }
            output.print("event: $event$newline")
            output.print("data: ")
            data.copyTo(output)
            output.print(newline)
            output.print(newline)
            asyncContext.response.flushBuffer()
        } catch (e: IOException) {
            closed = true
        }
    }

    fun emit(comment: String) = try {
        comment.split(newline).forEach {
            output.print("$COMMENT_PREFIX $it$newline")
        }
        asyncContext.response.flushBuffer()
    } catch (e: IOException) {
        closed = true
    }


    fun isClosed() = closed

}
