package io.javalin.http.sse

import java.io.IOException
import javax.servlet.AsyncContext
import javax.servlet.ServletOutputStream

class Emitter(private var asyncContext: AsyncContext) {

    private lateinit var output: ServletOutputStream
    private var close = false
    private val newline = "\n"

    init {
        try {
            this.output = asyncContext.response.outputStream
        } catch (e: IOException) {
            close = true
        }
    }

    @JvmOverloads
    fun emit(event: String, data: String, id: String? = null) = synchronized(this) {
        try {
            val sb = StringBuilder()
            if (id != null) {
                sb.append("id: $id$newline")
            }
            sb.append("event: $event$newline")
            data.lines().forEach { line ->
                sb.append("data: $line$newline")
            }
            sb.append("$newline$newline")
            output.print(sb.toString())
            asyncContext.response.flushBuffer()
        } catch (e: IOException) {
            close = true
        }
    }

    fun isClose() = close

}
