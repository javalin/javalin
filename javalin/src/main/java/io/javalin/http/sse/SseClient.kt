package io.javalin.http.sse

import io.javalin.http.Context

class SseClient(@JvmField val ctx: Context) {

    private val emitter: Emitter = Emitter(ctx.req.asyncContext)
    private var closeCallback: Runnable? = null

    fun onClose(closeCallback: Runnable) {
        this.closeCallback = closeCallback
    }

    fun sendEvent(data: String) = sendEvent("message", data)
    fun sendEvent(event: String, data: String) = sendEvent(event, data, null)
    fun sendEvent(event: String, data: String, id: String?) {
        emitter.emit(event, data, id)
        if (emitter.isClose() && closeCallback != null) {
            closeCallback!!.run()
        }
    }

}
