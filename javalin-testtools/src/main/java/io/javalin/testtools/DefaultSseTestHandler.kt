package io.javalin.testtools

abstract class DefaultSseTestHandler : SseTestHandler {

    override fun onMessage(sseEvent: SseEvent?) {
    }

    override fun onFailure(sseFailure: SseFailure) {
    }
}
