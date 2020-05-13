package io.javalin.http.sse

import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.Handler
import java.util.function.Consumer
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener

class SseHandler(private val clientConsumer: Consumer<SseClient>) : Handler {
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
            clientConsumer.accept(SseClient(ctx))
            ctx.req.asyncContext.addListener(object : AsyncListener {
                override fun onComplete(event: AsyncEvent) {}
                override fun onStartAsync(event: AsyncEvent) {}
                override fun onTimeout(event: AsyncEvent) = event.asyncContext.complete()
                override fun onError(event: AsyncEvent) = event.asyncContext.complete()
            })
        }
    }
}
