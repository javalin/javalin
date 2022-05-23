package io.javalin.testtools

import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.sse.EventSource

data class SseFailure(val client: OkHttpClient, val source: EventSource, val throwable: Throwable?, val response: Response?) {
    fun closeClient() = source.cancel().also { client.dispatcher.executorService.shutdownNow() }
}
