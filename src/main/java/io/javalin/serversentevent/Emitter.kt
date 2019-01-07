package io.javalin.serversentevent

import java.io.IOException
import javax.servlet.AsyncContext
import javax.servlet.ServletOutputStream
import java.io.StringReader
import java.io.BufferedReader



class Emitter(private var asyncContext: AsyncContext) {
    private var close: Boolean = false
    private var output: ServletOutputStream? = null
    private val CRLF = "\r\n"

    init {
        try {
            this.output = asyncContext.response.outputStream
        } catch (e: IOException) {
            close = true
        }
    }

    fun event(event: String, data: String) {
        synchronized(this) {
            try {
                output?.println("event: $event$CRLF")
                val reader = BufferedReader(StringReader(data))
                reader.lineSequence().forEach { line -> output?.println("data: $line$CRLF") }
                output?.println("$CRLF")
                asyncContext.response.flushBuffer()
            } catch (e: IOException) {
                close = true
            }
        }
    }

    fun isClose() = close

    fun event(id: Int, event: String, data: String) {
        synchronized(this) {
            try {
                output?.println("id: $id$CRLF")
                output?.println("event: $event$CRLF")
                output?.println("data: $data$CRLF")
                asyncContext.response.flushBuffer()
            } catch (e: IOException) {
                close = true
            }
        }
    }

}
