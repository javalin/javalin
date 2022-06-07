package io.javalin.http.sse

import io.javalin.http.Context
import io.javalin.plugin.json.jsonMapper
import java.io.Closeable
import java.io.InputStream

class SseClient internal constructor(
    @JvmField val ctx: Context
) : Closeable {

    private val emitter = Emitter(ctx.req.asyncContext)
    private var closeCallback = Runnable {}

    fun onClose(closeCallback: Runnable) {
        this.closeCallback = closeCallback
    }

    override fun close() {
        ctx.asyncComplete()
        closeCallback.run()
    }

    fun sendEvent(data: Any) = sendEvent("message", data)

    @JvmOverloads
    fun sendEvent(event: String, data: Any, id: String? = null) {
        when (data) {
            is InputStream -> emitter.emit(event, data, id)
            is String -> emitter.emit(event, data.byteInputStream(), id)
            else -> emitter.emit(event, ctx.jsonMapper().toJsonString(data).byteInputStream(), id)
        }
        if (emitter.isClosed()) { // can't detect if closed before we try emitting?
            this.close()
        }
    }

    fun sendComment(comment: String) {
        emitter.emit(comment)
        if (emitter.isClosed()) {
            this.close()
        }
    }

}
