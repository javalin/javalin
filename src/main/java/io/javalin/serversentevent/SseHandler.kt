package io.javalin.serversentevent

import io.javalin.Context
import io.javalin.Handler
import io.javalin.core.util.Header
import java.util.function.Consumer

class SseHandler(private val eventSourceConsumer: Consumer<EventSource>) : Handler {
    override fun handle(ctx: Context) {
        if (ctx.header(Header.ACCEPT) == "text/event-stream") {
            ctx.res.apply {
                status = 200
                characterEncoding = "UTF-8"
                contentType = "text/event-stream"
                addHeader(Header.CONNECTION, "close")
                addHeader(Header.CACHE_CONTROL, "no-cache")
                flushBuffer()
            }
            ctx.req.startAsync(ctx.req, ctx.res)
            ctx.req.asyncContext.timeout = 0
            eventSourceConsumer.accept(EventSource(ctx))
        }
    }
}
