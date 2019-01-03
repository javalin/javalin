package io.javalin.serversentevent

import io.javalin.Context

class EventSource(private val emitter: Emitter, private val context: Context) {

    private var close: SSEClose? = null

    fun onOpen(open: SSEConnect) {
        open(this)
    }

    fun onClose(close: SSEClose) {
        this.close = close
    }

    fun sendEvent(event: String, data: String) {
        emitter.event(event, data)
        if (emitter.isClose()) {
            close?.invoke(this)
        }
    }

    fun context(): Context {
        return context
    }
}
