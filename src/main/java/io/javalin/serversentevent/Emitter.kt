package io.javalin.serversentevent

import java.io.IOException
import javax.servlet.AsyncContext
import javax.servlet.ServletOutputStream
import java.io.StringReader
import java.io.BufferedReader



class Emitter(private var asyncContext: AsyncContext) {
    private var close: Boolean = false
    private var output: ServletOutputStream? = null
    private val CR = "\n"

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
                output?.println("event: $event$CR")
                val reader = BufferedReader(StringReader(data))
                reader.lineSequence().forEach { line -> output?.println("data: $line$CR") }
                output?.println("$CR$CR")
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
                output?.println("id: $id$CR")
                output?.println("event: $event$CR")
                output?.println("data: $data$CR")
                asyncContext.response.flushBuffer()
            } catch (e: IOException) {
                close = true
            }
        }
    }

}
