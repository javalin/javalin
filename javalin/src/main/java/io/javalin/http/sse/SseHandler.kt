package io.javalin.http.sse

import io.javalin.http.Context
import io.javalin.http.Handler
import io.javalin.http.Header
import java.util.function.Consumer

class SseHandler @JvmOverloads constructor(
    private val timeout: Long = 0,
    private val clientConsumer: Consumer<SseClient>
) : Handler {

    override fun handle(ctx: Context) {
        if (ctx.header(Header.ACCEPT)?.contains("text/event-stream") == true) {
            ctx.res().apply {
                status = 200
                characterEncoding = "UTF-8"
                contentType = "text/event-stream"
                addHeader(Header.CONNECTION, "close")
                addHeader(Header.CACHE_CONTROL, "no-cache")
                addHeader(Header.X_ACCEL_BUFFERING, "no") // See https://serverfault.com/a/801629
                flushBuffer()
            }
            ctx.async({
                it.timeout = timeout
            }) {
                clientConsumer.accept(SseClient(ctx))
            }
        }
    }

}
