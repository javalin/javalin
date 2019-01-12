package io.javalin.serversentevent

import java.io.BufferedReader
import java.io.IOException
import java.io.StringReader
import javax.servlet.AsyncContext
import javax.servlet.ServletOutputStream

class Emitter(private var asyncContext: AsyncContext) {

    private lateinit var output: ServletOutputStream
    private var close: Boolean = false
    private val CR = "\n"

    init {
        try {
            this.output = asyncContext.response.outputStream
        } catch (e: IOException) {
            close = true
        }
    }

    fun event(event: String, data: String) = synchronized(this) { //TODO: why is this
        try {
            output.println("event: $event$CR")
            BufferedReader(StringReader(data)).lineSequence().forEach { line -> output.println("data: $line$CR") }
            output.println("$CR$CR")
            asyncContext.response.flushBuffer()
        } catch (e: IOException) {
            close = true
        }
    }

    fun event(id: Int, event: String, data: String) = synchronized(this) { //TODO: not calling this?
        try {
            output.println("id: $id$CR")
            output.println("event: $event$CR")
            output.println("data: $data$CR")
            asyncContext.response.flushBuffer()
        } catch (e: IOException) {
            close = true
        }
    }

    fun isClose() = close

}
