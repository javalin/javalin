package io.javalin.serversentevent

import io.javalin.Context
import io.javalin.Handler

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.io.IOException
import java.nio.charset.Charset
import java.util.function.Consumer

class SSEHandler private constructor(private val consumerSSE: Consumer<EventSource>) : Handler {
    private val UTF_8 = Charset.forName("UTF-8")

    @Throws(Exception::class)
    override fun handle(context: Context) {
        val request = context.req
        val response = context.res

        val isEventStream = isEventStream(request)
        if (isEventStream) {
            startSSE(request, response)
            val configureSSE = createEventEmitter(request, context)
            consumerSSE.accept(configureSSE)
        }
    }

    private fun createEventEmitter(request: HttpServletRequest, context: Context): EventSourceImpl {
        val emitterEvent = EmitterImpl(request.asyncContext)
        val configureSSE = EventSourceImpl(emitterEvent, context.pathParamMap)
        return configureSSE
    }

    @Throws(IOException::class)
    private fun startSSE(request: HttpServletRequest, response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_OK
        response.characterEncoding = UTF_8.name()
        response.contentType = "text/event-stream"
        response.addHeader("Connection", "close")
        response.flushBuffer()
        request.startAsync(request, response)
    }

    protected fun isEventStream(request: HttpServletRequest): Boolean {
        return request.getHeader("Accept").equals("text/event-stream")
    }

    companion object {
        fun start(emitter: Consumer<EventSource>): Handler {
            return SSEHandler(emitter)
        }
    }
}
