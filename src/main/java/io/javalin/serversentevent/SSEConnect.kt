package io.javalin.serversentevent

@FunctionalInterface
interface SSEConnect {
    fun handler(eventSource: EventSource)
}