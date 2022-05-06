package io.javalin.http.sse

import io.javalin.http.Context
import io.javalin.plugin.json.jsonMapper
import java.io.Closeable
import java.io.InputStream

class SseClient(
    private val closeSse: CloseSseFunction,
    @JvmField val ctx: Context
) : Closeable {

    private val emitter = Emitter(ctx.req.asyncContext)
    private var closeCallback = Runnable {}

    fun onClose(closeCallback: Runnable) {
        this.closeCallback = closeCallback
    }

    fun sendEvent(data: Any) =
        sendEvent("message", data)

    override fun close() =
        closeSse.close()

    @JvmOverloads
    fun sendEvent(event: String, data: Any, id: String? = null) {
        when (data) {
            is InputStream -> emitter.emit(event, data, id)
            is String -> emitter.emit(event, data.byteInputStream(), id)
            else -> emitter.emit(event, ctx.jsonMapper().toJsonString(data).byteInputStream(), id)
        }
        if (emitter.isClosed()) { // can't detect if closed before we try emitting?
            closeCallback.run()
        }
    }

    fun sendComment(comment: String) {
        emitter.emit(comment)
        if (emitter.isClosed()) {
            closeCallback.run()
        }
    }

}
