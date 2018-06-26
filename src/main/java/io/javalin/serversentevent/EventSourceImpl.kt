package io.javalin.serversentevent

class EventSourceImpl(private val emitter: Emitter, private val pathParam: Map<String, String>) : EventSource {

    private var close: SSEClose? = null

    override fun onOpen(open: SSEConnect) {
        open(this)
    }

    override fun onClose(close: SSEClose) {
        this.close = close
    }

    override fun sendEvent(event: String, data: String) {
        emitter.event(event, data)
        if (emitter.isClose()) {
            close?.invoke(this)
        }
    }

    override fun pathParamMap(): Map<String, String> {
        return pathParam
    }
}
