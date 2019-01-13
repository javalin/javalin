package io.javalin.serversentevent

import java.io.IOException
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

    @JvmOverloads
    fun event(event: String, data: String, id: String? = null) = synchronized(this) {
        try {
            if (id != null) {
                output.println("id: $id$CR")
            }
            output.println("event: $event$CR")
            data.lines().forEach { line ->
                output.println("data: $line$CR")
            }
            output.println("$CR$CR")
            asyncContext.response.flushBuffer()
        } catch (e: IOException) {
            close = true
        }
    }

    fun isClose() = close

}
