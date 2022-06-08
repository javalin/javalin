package io.javalin.http.sse

import io.javalin.core.util.Header
import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.addListener
import java.io.Closeable
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import javax.servlet.AsyncEvent
import javax.servlet.AsyncListener

internal fun interface CloseSseFunction : Closeable

class SseHandler @JvmOverloads constructor(
    private val timeout: Long = 0,
    private val clientConsumer: Consumer<SseClient>
) : Handler {

    override fun handle(ctx: Context) {
        if (ctx.header(Header.ACCEPT) == "text/event-stream") {
            ctx.res.apply {
                status = 200
                characterEncoding = "UTF-8"
                contentType = "text/event-stream"
                addHeader(Header.CONNECTION, "close")
                addHeader(Header.CACHE_CONTROL, "no-cache")
                addHeader(Header.X_ACCEL_BUFFERING, "no") // See https://serverfault.com/a/801629
                flushBuffer()
            }
            ctx.req.startAsync(ctx.req, ctx.res)
            ctx.req.asyncContext.timeout = timeout

            ctx.req.asyncContext.addListener(
                onTimeout = { ctx.req.asyncContext.complete() },
                onError = { ctx.req.asyncContext.complete() }
            )

            clientConsumer.accept(SseClient({ ctx.req.asyncContext.complete() }, ctx))
        }
    }

}
