package io.javalin.serversentevent

import java.io.IOException
import javax.servlet.AsyncContext
import javax.servlet.ServletOutputStream

class EmitterImpl(asyncContext: AsyncContext) : Emitter {
    private var close: Boolean = false
    private var asyncContext: AsyncContext
    private var output: ServletOutputStream? = null
    private val CRLF = "\r\n"

    init {
        this.asyncContext = asyncContext
        try {
            this.output = asyncContext.response.outputStream
        } catch (e: IOException) {
            close = true
        }
    }

    override fun event(event: String, data: String) {
        synchronized(this) {
            try {
                output?.println("event: $event$CRLF")
                output?.println("data: $data$CRLF")
                asyncContext.response.flushBuffer()
            } catch (e: IOException) {
                close = true
            }
        }
    }

    override fun isClose(): Boolean {
        return close
    }
}