package io.javalin.testtools;

public interface SseTestHandler {

    void onMessage(SseEvent sseEvent);

    void onFailure(SseFailure sseFailure);

}
