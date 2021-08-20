package io.javalin.http.sse

import io.javalin.http.Context
import io.javalin.plugin.json.jsonMapper
import java.io.InputStream

class SseClient(@JvmField val ctx: Context) {

    private val emitter: Emitter = Emitter(ctx.req.asyncContext)
    private var closeCallback: Runnable = Runnable {}

    fun onClose(closeCallback: Runnable) {
        this.closeCallback = closeCallback
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
            closeCallback.run()
        }
    }

}
