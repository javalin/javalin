package io.javalin.testtools

import okhttp3.OkHttpClient
import okhttp3.sse.EventSource

data class SseEvent(val client: OkHttpClient, val source: EventSource, val id: String?, val type: String?, val data: String) {
    fun closeClient() = source.cancel().also { client.dispatcher.executorService.shutdownNow() }
}
