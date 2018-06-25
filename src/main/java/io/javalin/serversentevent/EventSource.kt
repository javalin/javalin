package io.javalin.serversentevent

interface EventSource {
    fun onOpen(connect: SSEConnect)
    fun onClose(close: SSEClose)
    fun sendEvent(event: String, data: String)
}