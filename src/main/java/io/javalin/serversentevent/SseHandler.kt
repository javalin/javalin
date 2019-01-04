package io.javalin.serversentevent

import io.javalin.Context
import io.javalin.Handler
import io.javalin.core.util.Header

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.io.IOException
import java.nio.charset.Charset
import java.util.function.Consumer

class SseHandler private constructor(private val consumerSse: Consumer<EventSource>) : Handler {
    override fun handle(context: Context) {
        if (isEventStream(context)) {
            startSSE(context.req, context.res)
            val configureSSE = createEventEmitter(context)
            consumerSse.accept(configureSSE)
        }
    }

    private fun createEventEmitter(context: Context): EventSource {
        val emitterEvent = Emitter(context.req.asyncContext)
        return EventSource(emitterEvent, context)
    }

    @Throws(IOException::class)
    private fun startSSE(request: HttpServletRequest, response: HttpServletResponse) {
        response.status = HttpServletResponse.SC_OK
        response.characterEncoding = Charset.forName("UTF-8").name()
        response.contentType = "text/event-stream"
        response.addHeader("Connection", "keep-alive")
        response.addHeader("Cache-Control","no-cache")
        response.flushBuffer()
        request.startAsync(request, response)
    }

    protected fun isEventStream(context: Context) = context.header(Header.ACCEPT).equals("text/event-stream")

    companion object {
        fun start(emitter: Consumer<EventSource>) = SseHandler(emitter)
    }
}
