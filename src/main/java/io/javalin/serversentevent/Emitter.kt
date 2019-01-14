package io.javalin.serversentevent

import java.io.IOException
import java.lang.StringBuilder
import javax.servlet.AsyncContext
import javax.servlet.ServletOutputStream

class Emitter(private var asyncContext: AsyncContext) {

    private lateinit var output: ServletOutputStream
    private var close = false
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
            val sb = StringBuilder()
            if (id != null) {
                sb.append("id: $id$CR")
            }
            sb.append("event: $event$CR")
            data.lines().forEach { line ->
                sb.append("data: $line$CR")
            }
            sb.append("$CR$CR")
            output.print(sb.toString())
            asyncContext.response.flushBuffer()
        } catch (e: IOException) {
            close = true
        }
    }

    fun isClose() = close

}
