package io.javalin.serversentevent

class EventSourceImpl(private val emitter: Emitter) : EventSource {
    private var close: SSEClose? = null

    override fun onOpen(open: SSEConnect) {
        open.handler(this)
    }

    override fun onClose(close: SSEClose) {
        this.close = close
    }

    override fun sendEvent(event: String, data: String) {
        emitter.event(event, data)
        if (emitter.isClose()) {
            close!!.handler(this)
        }
    }
}
