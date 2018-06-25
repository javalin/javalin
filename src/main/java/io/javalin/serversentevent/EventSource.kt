package io.javalin.serversentevent

interface EventSource {
    fun pathParamMap(): Map<String, String>
    fun onOpen(connect: SSEConnect)
    fun onClose(close: SSEClose)
    fun sendEvent(event: String, data: String)
}