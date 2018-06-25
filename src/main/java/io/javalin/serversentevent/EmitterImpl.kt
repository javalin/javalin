package io.javalin.serversentevent

import java.io.IOException
import javax.servlet.AsyncContext
import javax.servlet.ServletOutputStream

class EmitterImpl(asyncContext: AsyncContext): Emitter {
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
            sendEvent(event, data)
        }
    }

    private fun sendEvent(event: String, data: String) {
        synchronized(this) {
            try {
                event(event)
                data(data)
            } catch (e: IOException) {
                close = true
            }

        }
    }

    @Throws(IOException::class)
    private fun event(event: String) {
        synchronized(this) {
            output?.println("event: $event$CRLF")
        }
    }

    @Throws(IOException::class)
    private fun data(data: String) {
        synchronized(this) {
            output?.println("data: $data$CRLF")
            flush()
        }
    }

    @Throws(IOException::class)
    private fun flush() {
        asyncContext.response.flushBuffer()
    }

    override fun isClose(): Boolean {
       return close
    }
}