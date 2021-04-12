package io.javalin.http.sse

import io.javalin.http.Context

class SseClient(@JvmField val ctx: Context) {
    
    private val emitter: Emitter = Emitter(ctx.req.asyncContext)
    private var closeCallback: Runnable? = null

    fun onClose(closeCallback: Runnable) {
        this.closeCallback = closeCallback
    }

    @JvmOverloads
    fun sendEvent(event: String = "message", data: String, id: String? = null) {
        emitter.emit(event, data, id)
        if (emitter.isClose() && closeCallback != null) {
            closeCallback!!.run()
        }
    }

}
