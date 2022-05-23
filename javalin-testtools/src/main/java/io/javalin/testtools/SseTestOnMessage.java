package io.javalin.testtools;

import okhttp3.sse.EventSource;

public interface SseTestOnMessage {

    void process(EventSource eventSource, String id, String type, String data);

}
