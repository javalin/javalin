package io.javalin.serversentevent

@FunctionalInterface
interface SSEClose {
    fun handler(eventSource: EventSource)
}