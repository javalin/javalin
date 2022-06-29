package io.javalin.http.sse

import io.javalin.http.Context
import java.io.IOException
import java.io.InputStream
import jakarta.servlet.ServletOutputStream

const val COMMENT_PREFIX = ":"

class Emitter(private var context: Context) {

    private val output: ServletOutputStream by lazy {
        try {
            context.req.asyncContext.response.outputStream
        } catch (exception: Exception) {
            closed = true
            context.res.outputStream
        }}

    private var closed = false
    private val newline = "\n"

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
            context.req.asyncContext.response.flushBuffer()
        } catch (e: IOException) {
            closed = true
        }
    }

    fun emit(comment: String) = try {
        comment.split(newline).forEach {
            output.print("$COMMENT_PREFIX $it$newline")
        }
        context.req.asyncContext.response.flushBuffer()
    } catch (ignored: IOException) {
        closed = true
    }

    fun isClosed() = closed

}
