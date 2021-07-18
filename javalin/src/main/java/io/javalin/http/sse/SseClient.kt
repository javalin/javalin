package io.javalin.http.sse

import io.javalin.http.Context

class SseClient(@JvmField val ctx: Context) {

    private val emitter: Emitter = Emitter(ctx.req.asyncContext)
    private var closeCallback: Runnable = Runnable {}

    fun onClose(closeCallback: Runnable) {
        this.closeCallback = closeCallback
    }

    fun sendEvent(data: String) = sendEvent("message", data)

    @JvmOverloads
    fun sendEvent(event: String, data: String, id: String? = null) {
        emitter.emit(event, data, id)
        if (emitter.isClosed()) { // can't detect if closed before we try emitting?
            closeCallback.run()
        }
    }

}
